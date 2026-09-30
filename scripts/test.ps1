param([switch]$MySQL)
$ErrorActionPreference='Stop'
$workspacePath=Split-Path $PSScriptRoot -Parent
$previousTests=$env:RUN_MYSQL_TESTS
$previousMysqlPassword=$env:MYSQL_PWD
Push-Location -LiteralPath $workspacePath
try {
    if ($MySQL) {
        if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw '请先设置 DB_PASSWORD（测试库的 MySQL 密码）。' }
        if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) { $env:DB_USERNAME='root' }
        $env:MYSQL_PWD=$env:DB_PASSWORD
        & mysql --host=localhost --user=$env:DB_USERNAME --execute='CREATE DATABASE IF NOT EXISTS campus_takeaway_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
        if ($LASTEXITCODE -ne 0) { throw '无法创建专用测试库 campus_takeaway_test。' }
        $env:RUN_MYSQL_TESTS='true'
    } else {
        $env:RUN_MYSQL_TESTS='false'
    }
    & mvn test
    if ($LASTEXITCODE -ne 0) { throw '测试失败，详细结果见 target/surefire-reports。' }
} finally {
    $env:RUN_MYSQL_TESTS=$previousTests
    $env:MYSQL_PWD=$previousMysqlPassword
    Pop-Location
}
