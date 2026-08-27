param(
    [string]$MmceRoot = 'D:\Github\ModularMachinery-Community-Edition',
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$sourceGui = Join-Path $MmceRoot 'src\main\resources\assets\modularmachinery\textures\gui\mepatternprovider.png'
$sourceOverlay = Join-Path $MmceRoot 'src\main\resources\assets\modularmachinery\textures\blocks\overlay_mepatternprovider.png'
$targetGui = Join-Path $ProjectRoot 'src\main\resources\assets\mmce_complement\textures\gui\me_pattern_provider_ii.png'
$targetOverlay = Join-Path $ProjectRoot 'src\main\resources\assets\mmce_complement\textures\blocks\overlay_me_pattern_provider_ii.png'

if (-not (Test-Path -LiteralPath $sourceGui)) {
    throw "MMCE pattern-provider GUI was not found: $sourceGui"
}
if (-not (Test-Path -LiteralPath $sourceOverlay)) {
    throw "MMCE pattern-provider overlay was not found: $sourceOverlay"
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $targetGui) | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $targetOverlay) | Out-Null

function Draw-Region {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Bitmap]$Source,
        [int]$SourceX,
        [int]$SourceY,
        [int]$SourceWidth,
        [int]$SourceHeight,
        [int]$TargetX,
        [int]$TargetY,
        [int]$TargetWidth = $SourceWidth,
        [int]$TargetHeight = $SourceHeight
    )
    $destination = [System.Drawing.Rectangle]::new(
        $TargetX, $TargetY, $TargetWidth, $TargetHeight)
    $sourceRect = [System.Drawing.Rectangle]::new(
        $SourceX, $SourceY, $SourceWidth, $SourceHeight)
    $Graphics.DrawImage($Source, $destination, $sourceRect,
        [System.Drawing.GraphicsUnit]::Pixel)
}

$source = [System.Drawing.Bitmap]::new($sourceGui)
$target = [System.Drawing.Bitmap]::new(
    418, 268, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($target)
$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

try {
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $panel = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(255, 198, 198, 198))
    try {
        $graphics.FillRectangle($panel, 1, 1, 416, 266)
    } finally {
        $panel.Dispose()
    }

    # Rebuild the outer vanilla/MMCE bevel with nine-sliced, unscaled edges.
    Draw-Region $graphics $source 0 0 3 3 0 0
    Draw-Region $graphics $source 253 0 3 3 415 0
    Draw-Region $graphics $source 0 193 3 3 0 265
    Draw-Region $graphics $source 253 193 3 3 415 265
    Draw-Region $graphics $source 3 0 170 3 3 0 332 3
    Draw-Region $graphics $source 3 193 170 3 3 265 332 3
    Draw-Region $graphics $source 173 193 83 3 335 265
    Draw-Region $graphics $source 0 3 3 190 0 3 3 262
    Draw-Region $graphics $source 253 3 3 190 415 3 3 262

    # The cached-ingredient pane and scrollbar are copied pixel-for-pixel.
    # Its whole right pane moves as one piece; no part of it is scaled.
    Draw-Region $graphics $source 173 0 83 157 335 0

    # Continue the original three-pixel divider through the enlarged bottom.
    $dividerDark = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(255, 85, 85, 85))
    $dividerBlack = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::Black)
    try {
        $graphics.FillRectangle($dividerDark, 335, 157, 2, 108)
        $graphics.FillRectangle($dividerBlack, 337, 157, 1, 108)
    } finally {
        $dividerDark.Dispose()
        $dividerBlack.Dispose()
    }

    # Every slot is the original 18x18 MMCE slot frame at native resolution.
    for ($row = 0; $row -lt 8; $row++) {
        for ($column = 0; $column -lt 18; $column++) {
            Draw-Region $graphics $source 7 27 18 18 `
                (7 + $column * 18) (27 + $row * 18)
        }
    }

    # Player inventory: same relative layout as the original, shifted down by
    # the four additional pattern rows.
    for ($row = 0; $row -lt 3; $row++) {
        for ($column = 0; $column -lt 9; $column++) {
            Draw-Region $graphics $source 7 27 18 18 `
                (7 + $column * 18) (185 + $row * 18)
        }
    }
    for ($column = 0; $column -lt 9; $column++) {
        Draw-Region $graphics $source 7 27 18 18 `
            (7 + $column * 18) 243
    }

    # Expanded independent storage: a 3x3 item bank beside three fluid slots.
    $fluidMark = [System.Drawing.SolidBrush]::new(
        [System.Drawing.Color]::FromArgb(255, 200, 200, 200))
    try {
        for ($row = 0; $row -lt 3; $row++) {
            for ($column = 0; $column -lt 3; $column++) {
                Draw-Region $graphics $source 7 27 18 18 `
                    (342 + $column * 18) (207 + $row * 18)
            }
            $fluidSlotY = 207 + $row * 18
            Draw-Region $graphics $source 7 27 18 18 396 $fluidSlotY

            # Match MMCE's native 4x5 "F" in the bottom-right of a fluid slot.
            $markX = 408
            $markY = $fluidSlotY + 11
            $graphics.FillRectangle($fluidMark, $markX, $markY, 4, 1)
            $graphics.FillRectangle($fluidMark, $markX, $markY, 1, 5)
            $graphics.FillRectangle($fluidMark, $markX, $markY + 2, 3, 1)
        }
    } finally {
        $fluidMark.Dispose()
    }

    $target.Save($targetGui, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $target.Dispose()
    $source.Dispose()
}

# Retain the original provider face and divide its cream center into four.
# The recessed two-tone cross communicates the four-times pattern capacity
# without replacing the original center with a disconnected Roman-II mark.
$overlay = [System.Drawing.Bitmap]::new($sourceOverlay)
try {
    $separatorDark = [System.Drawing.Color]::FromArgb(255, 89, 89, 89)
    $separatorLight = [System.Drawing.Color]::FromArgb(255, 102, 102, 102)
    for ($y = 5; $y -le 10; $y++) {
        $overlay.SetPixel(7, $y, $separatorDark)
        $overlay.SetPixel(8, $y, $separatorLight)
    }
    for ($x = 5; $x -le 10; $x++) {
        $overlay.SetPixel($x, 7, $separatorDark)
        $overlay.SetPixel($x, 8, $separatorLight)
    }
    $overlay.Save($targetOverlay, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $overlay.Dispose()
}

Write-Output "Generated $targetGui"
Write-Output "Generated $targetOverlay"
