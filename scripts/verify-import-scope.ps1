$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checks = @(
    @{
        Path = "app/src/main/java/com/bu/kebiao/ui/courseimport/ImportScreen.kt"
        Pattern = "Excel导入|PDF导入|ExcelImportHint|PdfImportHint|OpenDocument|parseExcelFromStream|parsePdfFromStream|application/pdf|vnd\.ms-excel|spreadsheetml"
        Message = "ImportScreen still exposes PDF or Excel import UI."
    },
    @{
        Path = "app/src/main/java/com/bu/kebiao/ui/courseimport/ImportViewModel.kt"
        Pattern = "parseExcelFromStream|parsePdfFromStream|parseScheduleText|parseWeeksFromExcel|inferWeekType|StreamingReader|source = `"excel`"|source = `"pdf`""
        Message = "ImportViewModel still contains PDF or Excel parsing logic."
    },
    @{
        Path = "app/build.gradle.kts"
        Pattern = "pdfbox|excel\.streaming\.reader"
        Message = "Gradle dependencies still include PDF or Excel import libraries."
    },
    @{
        Path = "app/src/main/assets/school_index.json"
        Pattern = "GENERAL_TOOL_01|GENERAL_TOOL_02|组件测试|适配代码测试"
        Message = "School adapter index still includes testing presets."
    }
)

$failures = @()
foreach ($check in $checks) {
    $path = Join-Path $repoRoot $check.Path
    if (-not (Test-Path -LiteralPath $path)) {
        $failures += "Missing expected file: $($check.Path)"
        continue
    }

    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $path
    if ($content -match $check.Pattern) {
        $failures += $check.Message
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Import scope verification passed."
