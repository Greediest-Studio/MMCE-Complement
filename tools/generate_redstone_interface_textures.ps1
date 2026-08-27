Add-Type -AssemblyName System.Drawing

$textureRoot = Join-Path $PSScriptRoot `
    '..\src\main\resources\assets\mmce_complement\textures\blocks'
$textureRoot = [System.IO.Path]::GetFullPath($textureRoot)

function New-TransparentBitmap {
    $image = New-Object System.Drawing.Bitmap 16, 16,
        ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $image.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
        }
    }
    return $image
}

function Set-Pixels {
    param(
        [System.Drawing.Bitmap] $Image,
        [System.Drawing.Color] $Color,
        [array] $Pixels
    )
    foreach ($pixel in $Pixels) {
        $Image.SetPixel($pixel[0], $pixel[1], $Color)
    }
}

function Save-Texture {
    param([System.Drawing.Bitmap] $Image, [string] $Name)
    $path = Join-Path $textureRoot $Name
    $Image.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Image.Dispose()
}

$outline = [System.Drawing.Color]::FromArgb(255, 26, 30, 34)
$chip = [System.Drawing.Color]::FromArgb(255, 105, 116, 125)
$chipLight = [System.Drawing.Color]::FromArgb(255, 161, 172, 180)
$inputDark = [System.Drawing.Color]::FromArgb(255, 105, 24, 28)
$input = [System.Drawing.Color]::FromArgb(255, 207, 48, 49)
$inputLight = [System.Drawing.Color]::FromArgb(255, 255, 111, 91)
$outputDark = [System.Drawing.Color]::FromArgb(255, 122, 34, 20)
$output = [System.Drawing.Color]::FromArgb(255, 238, 77, 43)
$outputLight = [System.Drawing.Color]::FromArgb(255, 255, 161, 74)

function Draw-Chip {
    param([System.Drawing.Bitmap] $Image)
    Set-Pixels $Image $outline @(
        @(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),
        @(5,6),@(10,6),@(5,7),@(10,7),@(5,8),@(10,8),
        @(5,9),@(10,9),@(5,10),@(6,10),@(7,10),@(8,10),@(9,10),@(10,10)
    )
    Set-Pixels $Image $chip @(
        @(6,6),@(7,6),@(8,6),@(9,6),@(6,7),@(7,7),@(8,7),@(9,7),
        @(6,8),@(7,8),@(8,8),@(9,8),@(6,9),@(7,9),@(8,9),@(9,9)
    )
    Set-Pixels $Image $chipLight @(@(6,6),@(7,6),@(6,7))
}

# Input: four red traces terminate in arrowheads pointing into the grey chip.
$inputImage = New-TransparentBitmap
Draw-Chip $inputImage
Set-Pixels $inputImage $inputDark @(
    @(7,1),@(8,1),@(7,2),@(8,2),@(7,3),@(8,3),@(6,4),@(9,4),
    @(7,11),@(8,11),@(6,11),@(9,11),@(7,12),@(8,12),@(7,13),@(8,13),@(7,14),@(8,14),
    @(1,7),@(1,8),@(2,7),@(2,8),@(3,7),@(3,8),@(4,6),@(4,9),
    @(11,6),@(11,9),@(12,7),@(12,8),@(13,7),@(13,8),@(14,7),@(14,8)
)
Set-Pixels $inputImage $input @(
    @(7,2),@(7,3),@(6,4),@(7,4),@(8,4),@(9,4),
    @(6,11),@(7,11),@(8,11),@(9,11),@(7,12),@(7,13),
    @(2,7),@(3,7),@(4,6),@(4,7),@(4,8),@(4,9),
    @(11,6),@(11,7),@(11,8),@(11,9),@(12,7),@(13,7)
)
Set-Pixels $inputImage $inputLight @(@(7,2),@(6,4),@(2,7),@(4,6))
Save-Texture $inputImage 'overlay_redstone_signal_input.png'

# Output: the same chip drives four traces whose arrowheads point outward.
$outputImage = New-TransparentBitmap
Draw-Chip $outputImage
Set-Pixels $outputImage $outputDark @(
    @(7,1),@(8,1),@(6,2),@(9,2),@(7,3),@(8,3),@(7,4),@(8,4),
    @(7,11),@(8,11),@(7,12),@(8,12),@(6,13),@(9,13),@(7,14),@(8,14),
    @(1,7),@(1,8),@(2,6),@(2,9),@(3,7),@(3,8),@(4,7),@(4,8),
    @(11,7),@(11,8),@(12,7),@(12,8),@(13,6),@(13,9),@(14,7),@(14,8)
)
Set-Pixels $outputImage $output @(
    @(7,1),@(8,1),@(6,2),@(7,2),@(8,2),@(9,2),@(7,3),@(7,4),
    @(7,11),@(7,12),@(6,13),@(7,13),@(8,13),@(9,13),@(7,14),@(8,14),
    @(1,7),@(1,8),@(2,6),@(2,7),@(2,8),@(2,9),@(3,7),@(4,7),
    @(11,7),@(12,7),@(13,6),@(13,7),@(13,8),@(13,9),@(14,7),@(14,8)
)
Set-Pixels $outputImage $outputLight @(@(7,1),@(6,2),@(1,7),@(2,6))
Save-Texture $outputImage 'overlay_redstone_signal_output.png'
