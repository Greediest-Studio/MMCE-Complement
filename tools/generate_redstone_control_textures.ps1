Add-Type -AssemblyName System.Drawing

$textureRoot = Join-Path $PSScriptRoot `
    '..\src\main\resources\assets\mmce_complement\textures\blocks'
$textureRoot = [System.IO.Path]::GetFullPath($textureRoot)

function Set-Pixel {
    param(
        [System.Drawing.Bitmap] $Image,
        [int] $X,
        [int] $Y,
        [System.Drawing.Color] $Color
    )
    $Image.SetPixel($X, $Y, $Color)
}

function New-PowerTexture {
    param(
        [string] $OutputName,
        [string] $MainHex,
        [string] $LightHex,
        [string] $DarkHex
    )

    $image = New-Object System.Drawing.Bitmap 16, 16,
        ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $transparent = [System.Drawing.Color]::Transparent
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            Set-Pixel $image $x $y $transparent
        }
    }

    $outline = [System.Drawing.Color]::FromArgb(235, 18, 20, 22)
    $main = [System.Drawing.ColorTranslator]::FromHtml($MainHex)
    $light = [System.Drawing.ColorTranslator]::FromHtml($LightHex)
    $dark = [System.Drawing.ColorTranslator]::FromHtml($DarkHex)

    # Dark one-pixel silhouette keeps the icon readable over every machine tint.
    $outlinePixels = @(
        @(6, 2), @(7, 2), @(8, 2), @(9, 2),
        @(6, 3), @(9, 3), @(4, 4), @(5, 4), @(6, 4), @(9, 4), @(10, 4), @(11, 4),
        @(3, 5), @(4, 5), @(6, 5), @(9, 5), @(11, 5), @(12, 5),
        @(2, 6), @(3, 6), @(6, 6), @(9, 6), @(12, 6), @(13, 6),
        @(2, 7), @(3, 7), @(6, 7), @(7, 7), @(8, 7), @(9, 7), @(12, 7), @(13, 7),
        @(2, 8), @(3, 8), @(12, 8), @(13, 8),
        @(2, 9), @(3, 9), @(12, 9), @(13, 9),
        @(3, 10), @(4, 10), @(11, 10), @(12, 10),
        @(4, 11), @(5, 11), @(10, 11), @(11, 11),
        @(5, 12), @(6, 12), @(7, 12), @(8, 12), @(9, 12), @(10, 12),
        @(6, 13), @(7, 13), @(8, 13), @(9, 13)
    )
    foreach ($pixel in $outlinePixels) {
        Set-Pixel $image $pixel[0] $pixel[1] $outline
    }

    $mainPixels = @(
        @(7, 3), @(8, 3), @(7, 4), @(8, 4), @(5, 5), @(7, 5), @(8, 5), @(10, 5),
        @(4, 6), @(7, 6), @(8, 6), @(11, 6),
        @(3, 7), @(4, 7), @(11, 7), @(12, 7),
        @(3, 8), @(12, 8), @(3, 9), @(12, 9),
        @(4, 10), @(11, 10), @(5, 11), @(10, 11),
        @(6, 12), @(7, 12), @(8, 12), @(9, 12)
    )
    foreach ($pixel in $mainPixels) {
        Set-Pixel $image $pixel[0] $pixel[1] $main
    }

    foreach ($pixel in @(@(7, 3), @(5, 5), @(4, 6), @(3, 7), @(4, 10), @(5, 11), @(6, 12))) {
        Set-Pixel $image $pixel[0] $pixel[1] $light
    }
    foreach ($pixel in @(@(8, 6), @(11, 7), @(12, 8), @(12, 9), @(11, 10), @(10, 11), @(9, 12))) {
        Set-Pixel $image $pixel[0] $pixel[1] $dark
    }

    $output = Join-Path $textureRoot $OutputName
    $image.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
    $image.Dispose()
}

New-PowerTexture 'overlay_redstone_control_on.png' '#4DDB70' '#A2FFB4' '#17682D'
New-PowerTexture 'overlay_redstone_control_off.png' '#E14B48' '#FF9A8A' '#741B1C'
