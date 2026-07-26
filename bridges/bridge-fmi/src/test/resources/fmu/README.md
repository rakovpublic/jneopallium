# Test FMU Assets

This directory holds FMU files used by FMI bridge integration tests.

## How to regenerate

The FMUs are generated from Modelica source using OpenModelica:

```bash
# Install OpenModelica (https://openmodelica.org)
# Then from this directory:
omc +s +simCodeTarget=Cpp tank_temperature.mo
zip -r tank_temperature.fmu tank_temperature/

omc +s +simCodeTarget=Cpp cstr.mo
zip -r cstr.fmu cstr/
```

## tank_temperature.fmu

A simple heated-tank model for CI smoke tests:

```modelica
model TankTemperature
  parameter Real C = 5.0  "Thermal capacity [J/K]";
  parameter Real h = 1.0  "Heat loss coefficient [W/K]";
  parameter Real T_amb = 20.0  "Ambient temperature [deg C]";
  Real T(start = 20.0)  "Tank temperature [deg C]";
  input Real Q  "Heater power [W]";
  output Boolean over_temperature = T > 80.0;
equation
  C * der(T) = Q - h * (T - T_amb);
end TankTemperature;
```

Steady state: T_ss = T_amb + Q/h. At Q = 40 W → T_ss = 60 °C.  
Time constant τ = C/h = 5 s. Settles within ±0.5 °C of 60 °C after ~30 s.

## cstr.fmu

A continuously-stirred tank reactor (CSTR) for multi-variable control demos.
Regeneration details to be added when CSTR spec is finalised.

## Platform notes (R1 in 03-FMI-FMU.md §10)

FMUs must be cross-compiled for the CI target platform. The Linux x86-64
shared library must be at `binaries/linux64/<modelname>.so` inside the ZIP.
For macOS arm64: `binaries/darwin-arm64/<modelname>.dylib`.

The `StubFmuDriver` in `src/test/java/.../bridge/fmi/` runs without any
native binary and is used for all unit / integration tests.
