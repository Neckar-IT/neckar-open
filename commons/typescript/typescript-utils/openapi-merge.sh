#!/usr/bin/env bash

set -e

openRoot="$(cd "$(dirname "$0")/../../.." && pwd)"

# Renovate updates the version, so the CLI's own update notice carries nothing.
export REDOCLY_SUPPRESS_UPDATE_NOTICE=true

mkdir -p build/generated/open-api

pnpm exec redocly join \
  "$openRoot/rest/model/build/generated/openapi/main/openapi-rest-model.json" \
  "$openRoot/auth-rest-api/build/generated/openapi/main/openapi-auth.json" \
  "$openRoot/business-domain/build/generated/openapi/main/openapi-business-domain.json" \
  "$openRoot/customer/build/generated/openapi/main/openapi-customer.json" \
  -o build/generated/open-api/merged-openapi-source-files.json
