param([string]$InputDocx, [string]$OutputDirectory, [switch]$Update)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$resolvedDocx = (Resolve-Path -LiteralPath $InputDocx).Path
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDirectory).Path
$pdfPath = Join-Path $resolvedOutput 'report.pdf'
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $document = $word.Documents.Open($resolvedDocx, $false, (-not $Update))
    if ($Update) {
        $document.Repaginate()
        $null = $document.Fields.Update()
        $document.Repaginate()
        $null = $document.Fields.Update()
        $document.Save()
    }
    $document.ExportAsFixedFormat($pdfPath, 17)
    Write-Output ('Pages: ' + $document.ComputeStatistics(2))
    $document.Close($false)
} finally {
    $word.Quit()
}
& 'C:\Users\srikr\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\poppler\Library\bin\pdftoppm.exe' -png -r 85 $pdfPath (Join-Path $resolvedOutput 'page')
