#!/usr/bin/env bash

set -euo pipefail

COMPOSE_FILE="docker/observability/docker-compose.yml"

usage() {
  cat <<'EOF'
Usage: ./script/otel-local.sh <up|down|status|logs>

Commands:
  up      Start OTEL Collector + Tempo + Grafana
  down    Stop stack
  status  Show container status
  logs    Tail collector logs
EOF
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

case "$1" in
  up)
    docker compose -f "${COMPOSE_FILE}" up -d
    docker compose -f "${COMPOSE_FILE}" ps
    ;;
  down)
    docker compose -f "${COMPOSE_FILE}" down
    ;;
  status)
    docker compose -f "${COMPOSE_FILE}" ps
    ;;
  logs)
    docker compose -f "${COMPOSE_FILE}" logs -f otel-collector
    ;;
  *)
    usage
    exit 1
    ;;
esac
