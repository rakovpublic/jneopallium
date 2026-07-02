#!/usr/bin/env bash
# Initial (label-free) training + Python tests for the Self-Supervised
# Maintenance Guardian. The runtime model and its tests are Java (run with
# Maven: mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test).
set -euo pipefail
cd "$(dirname "$0")"

echo "== 1. initial (label-free) training =="
python train_ss_maintenance_model.py

echo
echo "== 2. python tests =="
python -m unittest tests.test_ss_maintenance -v
