#!/usr/bin/env bash
set -euo pipefail

base_url="${1:?Usage: $0 <base-url>}"
base_url="${base_url%/}"
failures=0

check() {
  local description="$1"
  shift
  if "$@"; then
    echo "PASS ${description}"
  else
    echo "FAIL ${description}" >&2
    failures=$((failures + 1))
  fi
}

wait_until_ready() {
  for _ in $(seq 1 30); do
    if curl -fsS --max-time 10 "${base_url}/actuator/health/readiness" | grep -q '"UP"'; then
      return 0
    fi
    sleep 10
  done
  return 1
}

status_of() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$@"
}

check "application becomes ready" wait_until_ready
check "readiness reports UP" bash -c "curl -fsS '${base_url}/actuator/health/readiness' | grep -q '\"UP\"'"
check "health hides component details" bash -c "! curl -fsS '${base_url}/actuator/health' | grep -q components"
check "frontend is served" bash -c "curl -fsS '${base_url}/' | grep -q 'KSM Connect'"
check "client-side route is served" test "$(status_of "${base_url}/events/00000000-0000-0000-0000-000000000000")" = "200"
check "anonymous session endpoint works" bash -c "curl -fsS '${base_url}/api/auth/session' | grep -q '\"authenticated\":false'"
check "public info reports database-backed API" test "$(status_of "${base_url}/api/public/info")" = "200"
check "participant API requires authentication" test "$(status_of "${base_url}/api/events")" = "401"
check "admin API requires authentication" test "$(status_of "${base_url}/api/admin/events")" = "401"
check "actuator internals are not exposed" test "$(status_of "${base_url}/actuator/env")" = "401"
check "state-changing request without CSRF token is rejected" test "$(status_of -X POST -H 'Content-Type: application/json' -d '{}' "${base_url}/api/auth/login")" = "403"
check "HTTP redirects or refuses plain text" bash -c "code=\$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 'http://${base_url#https://}/'); [[ \$code == 301 || \$code == 307 || \$code == 308 || \$code == 000 ]]"
check "HSTS header present" bash -c "curl -s -D - -o /dev/null --max-time 15 '${base_url}/' | grep -qi '^strict-transport-security'"
check "CSP header present" bash -c "curl -s -D - -o /dev/null --max-time 15 '${base_url}/' | grep -qi '^content-security-policy'"
check "frame embedding denied" bash -c "curl -s -D - -o /dev/null --max-time 15 '${base_url}/' | grep -qi '^x-frame-options: DENY'"
check "session cookie flags" bash -c "curl -s -D - -o /dev/null --max-time 15 '${base_url}/api/auth/csrf' | grep -i '^set-cookie: KSM_SESSION' | grep -qi 'secure' "
check "TLS 1.2+ certificate valid" bash -c "curl -fsS --tlsv1.2 -o /dev/null --max-time 15 '${base_url}/'"

if (( failures > 0 )); then
  echo "${failures} smoke check(s) failed." >&2
  exit 1
fi
echo "All smoke checks passed."
