$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$indexPath = Join-Path $repoRoot "app/src/main/assets/school_index.json"
$adaptersRoot = Join-Path $repoRoot "app/src/main/assets/adapters"

if (-not (Test-Path -LiteralPath $indexPath)) {
    Write-Error "Missing school_index.json."
    exit 1
}

$json = Get-Content -Raw -Encoding UTF8 -LiteralPath $indexPath | ConvertFrom-Json
$schools = @($json.schools)

if ($schools.Count -lt 100) {
    Write-Error "Expected restored upstream school presets, found only $($schools.Count) schools."
    exit 1
}

$requiredSchoolIds = @(
    "GLOBAL_TOOLS",
    "BUPT",
    "CQU",
    "ZJUT",
    "zhengfang_jiaowu",
    "chaoxing_jiaowu"
)

foreach ($requiredId in $requiredSchoolIds) {
    $school = $schools | Where-Object { $_.id -eq $requiredId } | Select-Object -First 1
    if (-not $school) {
        Write-Error "Missing required school preset: $requiredId."
        exit 1
    }
    if ([string]::IsNullOrWhiteSpace($school.name)) {
        Write-Error "School $requiredId has an empty name."
        exit 1
    }
    if (@($school.adapters).Count -lt 1) {
        Write-Error "School $requiredId has no adapters."
        exit 1
    }
}

$indexText = Get-Content -Raw -Encoding UTF8 -LiteralPath $indexPath
$forbiddenPatterns = @("GENERAL_TOOL_01", "GENERAL_TOOL_02")
foreach ($pattern in $forbiddenPatterns) {
    if ($indexText.Contains($pattern)) {
        Write-Error "School index still includes testing preset marker: $pattern."
        exit 1
    }
}

foreach ($school in $schools) {
    foreach ($adapter in @($school.adapters)) {
        if ([string]::IsNullOrWhiteSpace($adapter.js_path)) {
            Write-Error "Adapter $($adapter.adapter_id) in $($school.id) has empty js_path."
            exit 1
        }

        $scriptPath = Join-Path $adaptersRoot (Join-Path $school.folder $adapter.js_path)
        if (-not (Test-Path -LiteralPath $scriptPath)) {
            Write-Error "Missing adapter script for $($school.id)/$($adapter.adapter_id): $scriptPath"
            exit 1
        }
    }
}

Write-Host "School index verification passed: $($schools.Count) school presets."
