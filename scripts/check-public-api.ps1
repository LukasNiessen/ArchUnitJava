param(
    [string]$Jar = '',
    [string]$Baseline = 'docs/api/v1-public-api.txt',
    [switch]$Update
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not $Jar) {
    $candidates = @(Get-ChildItem -LiteralPath (Join-Path $repositoryRoot 'target') -Filter 'archunitjava-*.jar' |
        Where-Object { $_.Name -notmatch '-(?:sources|javadoc)\.jar$' })
    if ($candidates.Count -ne 1) { throw 'Build exactly one candidate JAR first, or supply -Jar.' }
    $Jar = $candidates[0].FullName
}
$Jar = [IO.Path]::GetFullPath($Jar)
$baselinePath = if ([IO.Path]::IsPathRooted($Baseline)) { $Baseline } else { Join-Path $repositoryRoot $Baseline }
$javap = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javap' } else { 'javap' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($Jar)
try {
    $classes = @($archive.Entries | Where-Object { $_.FullName -match '^dev/.*\.class$' } |
        ForEach-Object { $_.FullName.Replace('/', '.').Substring(0, $_.FullName.Length - 6) } |
        Sort-Object -CaseSensitive)
} finally { $archive.Dispose() }
if (-not $classes.Count) { throw 'The JAR contains no library classes.' }

# Small batches stay below Windows command-line limits. javap reads class-file bytes; it does
# not initialize library or application classes. Omit source filenames and normalize newlines.
$blocks = [Collections.Generic.List[string]]::new()
for ($offset = 0; $offset -lt $classes.Count; $offset += 40) {
    $last = [Math]::Min($offset + 39, $classes.Count - 1)
    $arguments = @('-protected', '-s', '-constants', '-classpath', $Jar) + $classes[$offset..$last]
    $output = & $javap @arguments
    if ($LASTEXITCODE -ne 0) { throw 'javap failed while extracting the public API.' }
    $text = ($output -join "`n") -replace '\r', ''
    foreach ($block in [regex]::Split($text, '(?m)^Compiled from [^\n]*\n')) {
        $value = $block.Trim()
        if ($value -match '^(?:public|protected) ') { $blocks.Add($value) }
    }
}
$blocks.Sort([StringComparer]::Ordinal)
$surface = "# ArchUnitJava v1 candidate public/protected API (javap, JDK 25)`n" +
    ($blocks -join "`n`n") + "`n"
if ($blocks.Count -eq 0) { throw 'No public API was extracted.' }
if ($Update) {
    New-Item -ItemType Directory -Force -Path (Split-Path $baselinePath) | Out-Null
    [IO.File]::WriteAllText($baselinePath, $surface, [Text.UTF8Encoding]::new($false))
    Write-Output "Updated API baseline: $($blocks.Count) public/protected types. Review the diff."
} else {
    if (-not (Test-Path -LiteralPath $baselinePath -PathType Leaf)) { throw 'API baseline is missing.' }
    $expected = [IO.File]::ReadAllText($baselinePath).Replace("`r`n", "`n")
    if ($surface -cne $expected) {
        $actualPath = Join-Path $repositoryRoot 'target/public-api-actual.txt'
        [IO.File]::WriteAllText($actualPath, $surface, [Text.UTF8Encoding]::new($false))
        throw "Public API changed. Compare $baselinePath with $actualPath; review compatibility before updating."
    }
    Write-Output "Public API matches: $($blocks.Count) public/protected types."
}
