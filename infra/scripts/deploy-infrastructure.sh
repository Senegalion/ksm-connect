#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <registry|environments> <what-if|apply> [image-reference]" >&2
  echo "Required env: ALERT_EMAIL. Optional: LOCATION (polandcentral), GITHUB_REPOSITORY (Senegalion/ksm-connect)," >&2
  echo "TEST_CUSTOM_HOSTNAME, PROD_CUSTOM_HOSTNAME, PROD_EMAIL_SENDER_ADDRESS, DEPLOY_TEST_ENVIRONMENT (false)." >&2
  exit 1
}

[[ $# -ge 2 ]] || usage
stage="$1"
action="$2"
image="${3:-}"
[[ "$stage" == "registry" || "$stage" == "environments" ]] || usage
[[ "$action" == "what-if" || "$action" == "apply" ]] || usage
[[ "$stage" == "registry" || -n "$image" ]] || { echo "The environments stage needs an image reference." >&2; exit 1; }

infra_dir="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "$infra_dir/prod.env" ]]; then
  set -a
  source "$infra_dir/prod.env"
  set +a
fi
: "${ALERT_EMAIL:?Set ALERT_EMAIL}"
location="${LOCATION:-polandcentral}"
repository="${GITHUB_REPOSITORY:-Senegalion/ksm-connect}"

az account show --query "{subscription:name, id:id}" -o table
deployer_id="$(az ad signed-in-user show --query id -o tsv)"

existing_secret() {
  local group="$1" name="$2" vault value=""
  vault="$(az keyvault list -g "$group" --query "[0].name" -o tsv 2>/dev/null || true)"
  if [[ -n "$vault" ]]; then
    value="$(az keyvault secret show --vault-name "$vault" -n "$name" --query value -o tsv 2>/dev/null || true)"
  fi
  printf '%s' "$value"
}

generated_secret() {
  printf '%sAa1-' "$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | cut -c1-40)"
}

secret_or_new() {
  local value
  value="$(existing_secret "$1" "$2")"
  value="${value#ksm:}"
  [[ -n "$value" ]] || value="$(generated_secret)"
  printf '%s' "$value"
}

parameters_file="$(mktemp)"
chmod 600 "$parameters_file"
trap 'rm -f "$parameters_file"' EXIT

deploy_environments=false
if [[ "$stage" == "environments" ]]; then
  deploy_environments=true
fi

TEST_PG="$(secret_or_new rg-ksm-test database-password)" \
TEST_PEPPER="$(secret_or_new rg-ksm-test token-pepper)" \
TEST_MAILPIT="$(secret_or_new rg-ksm-test mailpit-ui-auth)" \
PROD_PG="$(secret_or_new rg-ksm-prod database-password)" \
PROD_PEPPER="$(secret_or_new rg-ksm-prod token-pepper)" \
LOCATION_VALUE="$location" REPOSITORY_VALUE="$repository" DEPLOYER_VALUE="$deployer_id" \
ALERT_VALUE="$ALERT_EMAIL" DEPLOY_ENVIRONMENTS="$deploy_environments" IMAGE_VALUE="$image" DEPLOY_TEST="${DEPLOY_TEST_ENVIRONMENT:-false}" \
TEST_HOST="${TEST_CUSTOM_HOSTNAME:-}" PROD_HOST="${PROD_CUSTOM_HOSTNAME:-}" PROD_SENDER="${PROD_EMAIL_SENDER_ADDRESS:-}" \
PROD_EMAIL_DOMAIN="${PROD_EMAIL_CUSTOM_DOMAIN:-}" PROD_DOMAINS="${PROD_CUSTOM_DOMAINS:-}" \
python3 - "$parameters_file" <<'PYTHON'
import json, os, sys
value = lambda key: {"value": os.environ[key]}
parameters = {
    "location": value("LOCATION_VALUE"),
    "githubRepository": value("REPOSITORY_VALUE"),
    "deployerObjectId": value("DEPLOYER_VALUE"),
    "alertEmail": value("ALERT_VALUE"),
    "deployEnvironments": {"value": os.environ["DEPLOY_ENVIRONMENTS"] == "true"},
    "deployTestEnvironment": {"value": os.environ.get("DEPLOY_TEST", "false") == "true"},
    "backendImage": value("IMAGE_VALUE"),
    "testCustomHostname": value("TEST_HOST"),
    "prodCustomHostname": value("PROD_HOST"),
    "prodEmailSenderAddress": value("PROD_SENDER"),
    "prodEmailCustomDomain": value("PROD_EMAIL_DOMAIN"),
    "prodCustomDomains": {"value": [
        {"hostname": item.split("=")[0], "certificateName": item.split("=")[1]}
        for item in os.environ["PROD_DOMAINS"].split(",") if item
    ]},
    "testPostgresPassword": value("TEST_PG"),
    "testTokenPepper": value("TEST_PEPPER"),
    "testMailpitPassword": value("TEST_MAILPIT"),
    "prodPostgresPassword": value("PROD_PG"),
    "prodTokenPepper": value("PROD_PEPPER"),
}
with open(sys.argv[1], "w") as handle:
    json.dump({"$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#", "contentVersion": "1.0.0.0", "parameters": parameters}, handle)
PYTHON

deployment_name="ksm-${stage}-$(date +%Y%m%d%H%M%S)"
if [[ "$action" == "what-if" ]]; then
  az deployment sub what-if --name "$deployment_name" --location "$location" \
    --template-file "$infra_dir/main.bicep" --parameters "@$parameters_file" --result-format ResourceIdOnly
else
  az deployment sub create --name "$deployment_name" --location "$location" \
    --template-file "$infra_dir/main.bicep" --parameters "@$parameters_file" \
    --query "properties.outputs" -o json
fi
