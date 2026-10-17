# Regenerates assets/openapi-spec.js from openapi.yaml so swagger.html works offline (file://).
# Run after updating openapi.yaml:  pwsh .\docs\build-html.ps1
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$yaml = [IO.File]::ReadAllText((Join-Path $here 'openapi.yaml'), [Text.Encoding]::UTF8)
$json = $yaml | ConvertTo-Json -Compress
$content = "// Generated from openapi.yaml by build-html.ps1. Do not edit.`nwindow.OPENAPI_YAML = $json;`n"
[IO.File]::WriteAllText((Join-Path $here 'assets\openapi-spec.js'), $content, [Text.UTF8Encoding]::new($false))
Write-Output "Wrote assets\openapi-spec.js from openapi.yaml ($($yaml.Length) characters)"
