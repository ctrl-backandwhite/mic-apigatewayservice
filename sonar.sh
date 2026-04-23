#!/usr/bin/env bash
set -euo pipefail

# Run tests + jacoco + publish coverage to SonarCloud.
# Usage:
#   SONAR_TOKEN=xxx ./sonar.sh              # full verify + analysis
#   SONAR_TOKEN=xxx ./sonar.sh --no-tests   # skip tests, reuse existing jacoco-merged.xml

SONAR_HOST="${SONAR_HOST_URL:-https://sonarcloud.io}"
SONAR_PROJECT_KEY="ctrl-backandwhite_mic-apigatewayservice"
JACOCO_REPORT="target/site/jacoco-merged/jacoco.xml"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "::> SONAR_TOKEN is required." >&2
  exit 1
fi

cd "$(dirname "$0")"

if [[ "${1:-}" != "--no-tests" ]]; then
  mvn clean verify -Dsnyk.skip=true
fi

if [[ ! -f "$JACOCO_REPORT" ]]; then
  echo "::> $JACOCO_REPORT not found — run without --no-tests first." >&2
  exit 1
fi

mvn sonar:sonar \
  -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
  -Dsonar.host.url="$SONAR_HOST" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.coverage.jacoco.xmlReportPaths="$JACOCO_REPORT" \
  -Dsonar.coverage.exclusions='**/configuration/**,**/MicGatewayserviceApplication.java,**/domain/model/**,**/api/dto/**,**/infrastructure/entity/**' \
  -Dsnyk.skip=true
