#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CAMPAIGN_DIR="$SCRIPT_DIR/../campaign"
ACTION="${1:-once}"
cd "$CAMPAIGN_DIR"

case "$ACTION" in
  doctor) python -m jneo_campaign.cli.app doctor ;;
  dashboard) python -m jneo_campaign.cli.app dashboard ;;
  continuous) python -m jneo_campaign.cli.app run --continuous ;;
  once) python -m jneo_campaign.cli.app run --once ;;
  *) echo "usage: $0 {once|continuous|doctor|dashboard}" >&2; exit 2 ;;
esac
