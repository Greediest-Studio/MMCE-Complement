Add-Type -AssemblyName System.Drawing

function Color([string] $hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Set-Pixel(
    [System.Drawing.Bitmap] $image,
    [int] $x,
    [int] $y,
    [string] $hex) {
    $image.SetPixel($x, $y, (Color $hex))
}

function Set-Run(
    [System.Drawing.Bitmap] $image,
    [int] $y,
    [int] $fromX,
    [int] $toX,
    [string] $hex) {
    for ($x = $fromX; $x -le $toX; $x++) {
        Set-Pixel $image $x $y $hex
    }
}

function New-TransparentSprite {
    $image = New-Object System.Drawing.Bitmap 16, 16
    $image.MakeTransparent()
    return $image
}

function Draw-InterfacePlate([System.Drawing.Bitmap] $image) {
    # AE2's item icons use a compact, flat palette. This keeps the ME
    # Interface's square visual grammar without copying any MMCE bus overlay.
    Set-Pixel $image 2 2 '#999999'
    Set-Pixel $image 3 2 '#8C8C8C'
    Set-Run $image 2 4 11 '#A8A8A8'
    Set-Pixel $image 12 2 '#8C8C8C'
    Set-Pixel $image 13 2 '#999999'

    Set-Pixel $image 2 3 '#8C8C8C'
    Set-Run $image 3 3 12 '#404040'
    Set-Pixel $image 13 3 '#8C8C8C'

    for ($y = 4; $y -le 11; $y++) {
        Set-Pixel $image 2 $y '#A8A8A8'
        Set-Run $image $y 3 12 '#404040'
        Set-Pixel $image 13 $y '#A8A8A8'
    }

    Set-Pixel $image 2 12 '#8C8C8C'
    Set-Run $image 12 3 12 '#404040'
    Set-Pixel $image 13 12 '#8C8C8C'

    Set-Pixel $image 2 13 '#999999'
    Set-Pixel $image 3 13 '#8C8C8C'
    Set-Run $image 13 4 11 '#A8A8A8'
    Set-Pixel $image 12 13 '#8C8C8C'
    Set-Pixel $image 13 13 '#999999'

    # Flat inner screen matching AE2's original #646464 interface panel.
    for ($y = 5; $y -le 10; $y++) {
        Set-Run $image $y 5 10 '#646464'
    }
}

function Draw-ChannelMark(
    [System.Drawing.Bitmap] $image,
    [string] $channelColor,
    [string] $coreColor) {
    # Four equal traces converge on one neutral interface core. Every trace
    # uses one solid color; there is intentionally no lighting gradient.
    Set-Run $image 5 7 8 $channelColor
    Set-Run $image 6 7 8 $channelColor
    Set-Run $image 9 7 8 $channelColor
    Set-Run $image 10 7 8 $channelColor
    Set-Run $image 7 5 6 $channelColor
    Set-Run $image 8 5 6 $channelColor
    Set-Run $image 7 9 10 $channelColor
    Set-Run $image 8 9 10 $channelColor
    Set-Run $image 7 7 8 $coreColor
    Set-Run $image 8 7 8 $coreColor
}

# The hatch is a modified ME Interface plate mounted on the colorable machine
# casing. Cyan contact pixels extending through the panel frame identify it as
# a recipe-controlled channel input instead of an ordinary interface.
$overlay = New-TransparentSprite
Draw-InterfacePlate $overlay
Draw-ChannelMark $overlay '#55BFC5' '#C8E5E6'
Set-Run $overlay 3 7 8 '#55BFC5'
Set-Run $overlay 4 7 8 '#55BFC5'
Set-Run $overlay 11 7 8 '#55BFC5'
Set-Run $overlay 12 7 8 '#55BFC5'
Set-Run $overlay 7 3 4 '#55BFC5'
Set-Run $overlay 8 3 4 '#55BFC5'
Set-Run $overlay 7 11 12 '#55BFC5'
Set-Run $overlay 8 11 12 '#55BFC5'

$overlayOutput = Join-Path $PSScriptRoot `
    '..\src\main\resources\assets\mmce_complement\textures\blocks\overlay_me_channel_input_hatch.png'
$overlayOutput = [System.IO.Path]::GetFullPath($overlayOutput)
$overlay.Save($overlayOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$overlay.Dispose()

# JEI uses the compact item-like form: the same ME Interface-derived plate,
# but only the inner channel mark, leaving room for the amount text overlay.
$jei = New-TransparentSprite
Draw-InterfacePlate $jei
Draw-ChannelMark $jei '#55BFC5' '#C8E5E6'

$jeiOutput = Join-Path $PSScriptRoot `
    '..\src\main\resources\assets\mmce_complement\textures\gui\me_channel_ingredient.png'
$jeiOutput = [System.IO.Path]::GetFullPath($jeiOutput)
$jei.Save($jeiOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$jei.Dispose()
