#!/usr/bin/env bash
# Starts the Vite dev server of this package. This dev server is the source of the deployed showcase
# react-ui.showcase.neckar.it.
cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1
exec pnpm dev "$@"
