param([switch]$MySQL)
$ErrorActionPreference='Stop'
$workspacePath=Split-Path $PSScriptRoot -Parent
$previousTests=$env:RUN_MYSQL_TESTS
Push-Location -LiteralPath $workspacePath
try {
    $env:RUN_MYSQL_TESTS=if ($MySQL) { 'true' } else { 'false' }
    & npm.cmd test --prefix web
    if ($LASTEXITCODE -ne 0) { throw '前端检查失败。' }
    & npm.cmd run build --prefix web
    if ($LASTEXITCODE -ne 0) { throw 'Vue 构建失败。' }
    & mvn verify
    if ($LASTEXITCODE -ne 0) { throw '验证失败，详细结果见 target/surefire-reports。' }
} finally {
    $env:RUN_MYSQL_TESTS=$previousTests
    Pop-Location
}
