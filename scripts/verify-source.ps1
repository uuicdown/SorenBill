$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$files = @(
    Get-ChildItem -Path (Join-Path $root "app/src/main") -Recurse -Include *.kt,*.xml
)

$issues = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
    $lineNo = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName -Encoding UTF8) {
        $lineNo++
        $relative = $file.FullName.Substring($root.Length + 1)

        if ($line -match 'android:label="[^"]*$') {
            $issues.Add("${relative}:${lineNo}: unclosed android:label attribute")
        }

        if ($line -match '//.*\b(if|viewModelScope\.launch|item)\s*\{') {
            $issues.Add("${relative}:${lineNo}: executable code appears to be swallowed by a comment")
        }

        if ($line -match '"[^"]*\?[,)]') {
            $issues.Add("${relative}:${lineNo}: suspicious truncated string literal")
        }

        if ($line -match '"[^"]*$' -and $line -notmatch '^\s*//' -and $line -notmatch '"""') {
            $quoteCount = ([regex]::Matches($line, '"')).Count
            if (($quoteCount % 2) -eq 1) {
                $issues.Add("${relative}:${lineNo}: odd number of double quotes")
            }
        }
    }
}

if ($issues.Count -gt 0) {
    Write-Host "Source verification failed:"
    $issues | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Source verification passed."
