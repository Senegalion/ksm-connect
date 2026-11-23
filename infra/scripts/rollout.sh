#!/usr/bin/env bash
set -euo pipefail

group="${1:?Usage: $0 <resource-group> <container-app> <image>}"
app="${2:?}"
image="${3:?}"
timeout_seconds="${ROLLOUT_TIMEOUT_SECONDS:-600}"

previous_image="$(az containerapp show -g "$group" -n "$app" --query "properties.template.containers[0].image" -o tsv)"
echo "Previous image: ${previous_image}"
echo "Deploying image: ${image}"

revision="$(az containerapp update -g "$group" -n "$app" --image "$image" --query "properties.latestRevisionName" -o tsv)"
echo "New revision: ${revision}"

deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  state="$(az containerapp revision show -g "$group" -n "$app" --revision "$revision" \
    --query "{health:properties.healthState, running:properties.runningState, provisioning:properties.provisioningState}" -o tsv)"
  read -r health running provisioning <<<"$state"
  echo "Revision ${revision}: health=${health} running=${running} provisioning=${provisioning}"
  if [[ "$provisioning" == "Failed" || "$running" == "Failed" || "$health" == "Unhealthy" ]]; then
    echo "Revision failed; traffic stays on the previous healthy revision. Previous image: ${previous_image}" >&2
    exit 1
  fi
  if [[ "$health" == "Healthy" && ( "$running" == "Running" || "$running" == "RunningAtMaxScale" || "$running" == "Activating" || "$running" == "ScaledToZero" ) ]]; then
    echo "Revision ${revision} is healthy."
    exit 0
  fi
  sleep 10
done

echo "Timed out waiting for revision ${revision}." >&2
exit 1
