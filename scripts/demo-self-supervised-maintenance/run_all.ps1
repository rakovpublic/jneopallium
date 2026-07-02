# Initial (label-free) training + Python tests for the Self-Supervised
# Maintenance Guardian. The runtime model and its tests are Java (run with
# Maven: mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "== 1. initial (label-free) training =="
python train_ss_maintenance_model.py

Write-Host ""
Write-Host "== 2. python tests =="
python -m unittest tests.test_ss_maintenance -v
