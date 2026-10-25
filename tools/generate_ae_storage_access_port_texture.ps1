Add-Type -AssemblyName System.Drawing

# Pixel-art hatch overlay in the existing MMCE casing/overlay system.
# The two-pixel transparent margin leaves the machine's colorable casing visible.
$sprite = New-Object System.Drawing.Bitmap 16, 16
$sprite.MakeTransparent()
$palette = @{
    '.' = [System.Drawing.Color]::Transparent
    'd' = [System.Drawing.ColorTranslator]::FromHtml('#363B40')
    'g' = [System.Drawing.ColorTranslator]::FromHtml('#666C70')
    's' = [System.Drawing.ColorTranslator]::FromHtml('#A0A5A6')
    'l' = [System.Drawing.ColorTranslator]::FromHtml('#C3C8C8')
    'c' = [System.Drawing.ColorTranslator]::FromHtml('#61ABB1')
    'w' = [System.Drawing.ColorTranslator]::FromHtml('#B3D9D6')
}
$pixels = @(
    '................',
    '................',
    '...sggggggggs...',
    '..sgddddddddgs..',
    '..gdlggggggldg..',
    '..gdgccccccgdg..',
    '..cddggggggddc..',
    '..wddllllllddw..',
    '..wddllllllddw..',
    '..cddggggggddc..',
    '..gdgccccccgdg..',
    '..gdlggggggldg..',
    '..sgddddddddgs..',
    '...sggggggggs...',
    '................',
    '................'
)
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $sprite.SetPixel($x, $y, $palette[[string]$pixels[$y][$x]])
    }
}
$destination = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mmce_complement\textures\blocks\overlay_ae_storage_access_port.png'))
$sprite.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
$sprite.Dispose()
