param(
    [string]$OrePng,
    [string]$GlowPng,
    [string]$Out
)

Add-Type -AssemblyName System.Drawing

$ore = [System.Drawing.Bitmap]::FromFile($OrePng)
$glow = [System.Drawing.Bitmap]::FromFile($GlowPng)

# 1) 从用户的矿石贴图里取矿点主色（高亮且偏黄绿的像素）
$sumR = 0; $sumG = 0; $sumB = 0; $count = 0
for ($x = 0; $x -lt $ore.Width; $x++) {
    for ($y = 0; $y -lt $ore.Height; $y++) {
        $c = $ore.GetPixel($x, $y)
        $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
        if ($lum -ge 140) {
            $sumR += $c.R; $sumG += $c.G; $sumB += $c.B; $count++
        }
    }
}
if ($count -eq 0) { $count = 1 }
$baseR = [int]($sumR / $count)
$baseG = [int]($sumG / $count)
$baseB = [int]($sumB / $count)

# 2) 用原发光层的形状/明暗，替换成新色相
$result = New-Object System.Drawing.Bitmap($glow.Width, $glow.Height)
for ($x = 0; $x -lt $glow.Width; $x++) {
    for ($y = 0; $y -lt $glow.Height; $y++) {
        $g = $glow.GetPixel($x, $y)
        if ($g.A -eq 0) {
            $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            continue
        }
        # 原发光层的相对亮度（0~1）
        $lum = [Math]::Max($g.R, [Math]::Max($g.G, $g.B)) / 255.0
        # 低亮度压暗、高亮度提亮，保证自发光层够亮
        $k = 0.45 + $lum * 0.85
        $r = [Math]::Min(255, [int]($baseR * $k + 18))
        $gg = [Math]::Min(255, [int]($baseG * $k + 18))
        $b = [Math]::Min(255, [int]($baseB * $k + 10))
        $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($g.A, $r, $gg, $b))
    }
}

$result.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$result.Dispose(); $ore.Dispose(); $glow.Dispose()

Write-Output "矿点采样色: R=$baseR G=$baseG B=$baseB  ->  $Out"

