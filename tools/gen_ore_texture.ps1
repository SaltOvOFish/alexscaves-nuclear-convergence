param(
    [string]$Base,
    [string]$RefBase,
    [string]$RefOre,
    [string]$Out,
    [int]$LightR, [int]$LightG, [int]$LightB,
    [int]$DarkR, [int]$DarkG, [int]$DarkB
)

Add-Type -AssemblyName System.Drawing

$baseBmp = [System.Drawing.Bitmap]::FromFile($Base)
$refBaseBmp = [System.Drawing.Bitmap]::FromFile($RefBase)
$refOreBmp = [System.Drawing.Bitmap]::FromFile($RefOre)
$outBmp = New-Object System.Drawing.Bitmap($baseBmp.Width, $baseBmp.Height)

$w = $baseBmp.Width
$h = $baseBmp.Height
$count = 0

for ($x = 0; $x -lt $w; $x++) {
    for ($y = 0; $y -lt $h; $y++) {
        $b = $baseBmp.GetPixel($x, $y)
        $rb = $refBaseBmp.GetPixel($x, $y)
        $ro = $refOreBmp.GetPixel($x, $y)

        $dist = [Math]::Abs($ro.R - $rb.R) + [Math]::Abs($ro.G - $rb.G) + [Math]::Abs($ro.B - $rb.B)
        if ($dist -gt 40) {
            $lum = (0.299 * $ro.R + 0.587 * $ro.G + 0.114 * $ro.B) / 255.0
            $t = [Math]::Max(0.0, [Math]::Min(1.0, ($lum - 0.15) / 0.55))
            $r = [int]($DarkR + ($LightR - $DarkR) * $t)
            $g = [int]($DarkG + ($LightG - $DarkG) * $t)
            $bl = [int]($DarkB + ($LightB - $DarkB) * $t)
            $outBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $bl))
            $count++
        } else {
            $outBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $b.R, $b.G, $b.B))
        }
    }
}

$outBmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$outBmp.Dispose(); $baseBmp.Dispose(); $refBaseBmp.Dispose(); $refOreBmp.Dispose()
Write-Output "生成 $Out（矿点像素 $count 个）"
