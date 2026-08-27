param(
    [string] $ReferenceTexture = ''
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($ReferenceTexture)) {
    $cacheRoot = Join-Path $env:USERPROFILE `
        '.gradle\caches\minecraft\deobfedDeps\deobf\curse\maven\modularmachinery-community-edition-817377'
    $dependencyJar = Get-ChildItem $cacheRoot -Recurse -Filter '*.jar' `
        -ErrorAction Stop | Sort-Object LastWriteTime -Descending `
        | Select-Object -First 1
    if ($null -eq $dependencyJar) {
        throw 'Unable to locate the Modular Machinery CE dependency JAR.'
    }

    $archive = [System.IO.Compression.ZipFile]::OpenRead(
        $dependencyJar.FullName)
    try {
        $entry = $archive.GetEntry(
            'assets/modularmachinery/textures/gui/meiteminputbus.png')
        if ($null -eq $entry) {
            throw 'MMCE meiteminputbus.png was not found in the dependency JAR.'
        }
        $entryStream = $entry.Open()
        try {
            $source = New-Object System.Drawing.Bitmap $entryStream
            $image = New-Object System.Drawing.Bitmap $source
            $source.Dispose()
        } finally {
            $entryStream.Dispose()
        }
    } finally {
        $archive.Dispose()
    }
} else {
    $source = New-Object System.Drawing.Bitmap $ReferenceTexture
    $image = New-Object System.Drawing.Bitmap $source
    $source.Dispose()
}

$graphics = [System.Drawing.Graphics]::FromImage($image)
try {
    $panel = New-Object System.Drawing.SolidBrush `
        ([System.Drawing.Color]::FromArgb(255, 198, 198, 198))
    $graphics.FillRectangle($panel, 4, 20, 168, 101)
    $panel.Dispose()

    $slotDark = [System.Drawing.Color]::FromArgb(255, 55, 55, 55)
    $slotLight = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
    $slotInside = [System.Drawing.Color]::FromArgb(255, 138, 138, 138)

    for ($row = 0; $row -lt 4; $row++) {
        for ($column = 0; $column -lt 4; $column++) {
            $x = 52 + $column * 18
            $y = 35 + $row * 18

            for ($pixel = -1; $pixel -le 16; $pixel++) {
                $image.SetPixel($x + $pixel, $y - 1, $slotDark)
                $image.SetPixel($x - 1, $y + $pixel, $slotDark)
                $image.SetPixel($x + $pixel, $y + 16, $slotLight)
                $image.SetPixel($x + 16, $y + $pixel, $slotLight)
            }
            for ($innerY = 0; $innerY -lt 16; $innerY++) {
                for ($innerX = 0; $innerX -lt 16; $innerX++) {
                    $image.SetPixel($x + $innerX, $y + $innerY,
                        $slotInside)
                }
            }
        }
    }
} finally {
    $graphics.Dispose()
}

$output = Join-Path $PSScriptRoot `
    '..\src\main\resources\assets\mmce_complement\textures\gui\me_output_assembly.png'
$output = [System.IO.Path]::GetFullPath($output)
$outputDirectory = Split-Path $output -Parent
if (!(Test-Path $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}
$image.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$image.Dispose()
