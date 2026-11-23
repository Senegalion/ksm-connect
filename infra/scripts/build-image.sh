#!/usr/bin/env bash
set -euo pipefail

registry="${1:?Usage: $0 <registry-name>}"
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree has uncommitted changes; commit them so the image maps to an exact revision." >&2
  exit 1
fi

tag="$(git rev-parse HEAD)"
az acr build --registry "$registry" --image "ksm-connect:${tag}" --file Dockerfile . >&2
digest="$(az acr repository show --name "$registry" --image "ksm-connect:${tag}" --query digest -o tsv)"
login_server="$(az acr show --name "$registry" --query loginServer -o tsv)"
echo "${login_server}/ksm-connect@${digest}"
