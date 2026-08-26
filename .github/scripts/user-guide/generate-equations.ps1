# ---------------------------------------------------------------------------------------------------------------------
# Project: ECA Rule Engine 2
# Version: 2.0
# Date:    2025
# Author:  Rohin Gosling
#
# Description:
#
#   Rebuilds the native-safe PNG equations displayed by the desktop and web context-sensitive help panels.
#
#   The equations are rendered with a fully transparent background so that the glyphs composite correctly over any
#   surface color the surrounding panel happens to use. Only the glyph color is baked in, which is why a light and a
#   dark variant of every equation is still produced.
#
# ---------------------------------------------------------------------------------------------------------------------

[CmdletBinding()]
param (
    [switch] $KeepTemporary
)

# Perform the set strict mode calls required by the script operation.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Perform the add type calls required by the script operation.

Add-Type -AssemblyName System.Drawing

function Find-EquationExecutable
{
    param (
        [Parameter(Mandatory = $true)]
        [string] $CommandName,

        [Parameter(Mandatory = $true)]
        [string[]] $FallbackPaths
    )

    # Process each value supplied by the selected collection.

    foreach ( $fallbackPath in $FallbackPaths )
    {
        # Handle the branch where test path literal path fallback path.

        if ( Test-Path -LiteralPath $fallbackPath )
        {
            # Return the fallback path to the caller.

            return $fallbackPath
        }
    }

    # Initialize the command by applying get command and select object.

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1

    # Handle the branch where command is available.

    if ( $null -ne $command )
    {
        # Return the prepared result to the caller.

        return $command.Source
    }

    throw "Could not locate $CommandName. Install MiKTeX or add the executable to PATH."
}

function Set-EquationImagePerimeter
{
    param (
        [Parameter(Mandatory = $true)]
        [string] $ImagePath
    )

    # Prepare the transparent and normalized image path values required by the set equation image perimeter operation.
    # The renderer can leave a faint anti-aliased edge row or column behind, so the perimeter is forced to fully
    # transparent pixels to guarantee a clean composite against any panel background.

    $transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
    $normalizedImagePath = $ImagePath + '.normalized.png'

    # Initialize the source image by applying from file.

    $sourceImage = [System.Drawing.Bitmap]::FromFile($ImagePath)

    # Run the protected operation and route failures through the matching handler.

    try
    {
        # Initialize the image by applying new, so that the working surface always carries a straight alpha channel.

        $image = [System.Drawing.Bitmap]::new(
            $sourceImage,
            $sourceImage.Width,
            $sourceImage.Height
        )

        # Run the protected operation and route failures through the matching handler.

        try
        {
            # Repeat the indexed loop while its condition remains true.

            for ( $x = 0; $x -lt $image.Width; $x++ )
            {
                # Perform the set pixel calls required by the set equation image perimeter operation.

                $image.SetPixel($x, 0, $transparent)
                $image.SetPixel($x, $image.Height - 1, $transparent)
            }

            # Repeat the indexed loop while its condition remains true.

            for ( $y = 1; $y -lt $image.Height - 1; $y++ )
            {
                # Perform the set pixel calls required by the set equation image perimeter operation.

                $image.SetPixel(0, $y, $transparent)
                $image.SetPixel($image.Width - 1, $y, $transparent)
            }

            # Perform the save calls required by the set equation image perimeter operation.

            $image.Save(
                $normalizedImagePath,
                [System.Drawing.Imaging.ImageFormat]::Png
            )
        }

        # Complete the required cleanup regardless of how the protected operation finishes.

        finally
        {
            # Perform the dispose calls required by the set equation image perimeter operation.

            $image.Dispose()
        }
    }

    # Complete the required cleanup regardless of how the protected operation finishes.

    finally
    {
        # Perform the dispose calls required by the set equation image perimeter operation.

        $sourceImage.Dispose()
    }

    # Perform the move item calls required by the set equation image perimeter operation.

    Move-Item -LiteralPath $normalizedImagePath -Destination $ImagePath -Force
}

# Prepare the mi kte xdirectory and latex executable values required by the script operation.

$miKTeXDirectory = Join-Path $env:LOCALAPPDATA 'Programs\MiKTeX\miktex\bin\x64'
$latexExecutable = Find-EquationExecutable `
    -CommandName 'xelatex' `
    -FallbackPaths @( ( Join-Path $miKTeXDirectory 'xelatex.exe' ) )

# Initialize the pdf to cairo executable by applying find equation executable. The cairo renderer is used in place of
# the portable pixmap renderer because it is the one that can emit a transparent PNG background.

$pdfToCairoExecutable = Find-EquationExecutable `
    -CommandName 'pdftocairo' `
    -FallbackPaths @( ( Join-Path $miKTeXDirectory 'pdftocairo.exe' ) )

# Prepare the project directory, output directories, and temporary directory values required by the script operation.

$projectDirectory = ( Resolve-Path -LiteralPath ( Join-Path $PSScriptRoot '..\..\..' ) ).Path
$outputDirectories = @(
    ( Join-Path `
        $projectDirectory `
        'eca-client\src\main\resources\com\rohingosling\eca\client\user-guide' ),
    ( Join-Path `
        $projectDirectory `
        'eca-web\public\assets\equations' )
)
$temporaryDirectory = Join-Path `
    ([System.IO.Path]::GetTempPath()) `
    ( 'eca-user-guide-' + [System.Guid]::NewGuid().ToString('N') )

$equations = [ordered] @{
    'model' = @'
\begin{aligned}
Q_{\tau,R}&:\mathcal{X}\longrightarrow\mathcal{A}_{\bot},\\
Q_{\tau,R}(x)&=Q_\tau(x,R)
\end{aligned}
'@
    'parameters' = @'
\begin{aligned}
\overline{V}_k&=V_k\uplus\{\nu\},\\
(k,v),\quad k&\in K,\quad v\in\overline{V}_k
\end{aligned}
'@
    'payloads' = @'
\begin{aligned}
\operatorname{dom}(p)&\subseteq K_e,\\
p(k)&\in\overline{V}_k
\quad\text{for every }k\in\operatorname{dom}(p)
\end{aligned}
'@
    'events' = @'
\begin{aligned}
x&=(e,p),\\
\mathcal{X}
&=\{(e,p)\mid e\in\mathcal{E}\land p\in\mathcal{P}_e\}
\end{aligned}
'@
    'conditions' = @'
\begin{aligned}
\widehat{V}_k&=V_k\uplus\{\ast\},\\
(k,q),\quad k&\in K,\quad q\in\widehat{V}_k
\end{aligned}
'@
    'condition-sets' = @'
\begin{aligned}
\mu(c,p)=\mathsf{true}
&\Longleftrightarrow
\forall k\in\operatorname{dom}(c):\\
&c(k)=\ast\;\lor\;
\left(
\begin{aligned}
k&\in\operatorname{dom}(p)\\[-1pt]
{}\land\;c(k)&=p(k)\in V_k
\end{aligned}
\right),\\[2pt]
\sigma(c)&=2n_{\mathrm{con}}(c)+n_{\mathrm{wild}}(c)
\end{aligned}
'@
    'actions' = @'
\mathcal{A}_{\bot}
=
\mathcal{A}\uplus\{\bot\}
'@
    'rules' = @'
r=(i,e,c,a)
\in
\mathcal{I}\times\mathcal{E}\times\mathcal{C}\times\mathcal{A}
'@
    'simulator' = @'
Q_\tau(x,R)
=
\begin{cases}
\bot,
&L_R(x)=\varnothing,\\[4pt]
\operatorname{action}\!\left(w_{\tau,R}(x)\right),
&L_R(x)\ne\varnothing
\end{cases}
'@
}

$equationThemes = [ordered] @{
    '' = @{
        ForegroundColor = '17212B'
    }
    '-dark' = @{
        ForegroundColor = 'F3F3F3'
    }
}

# Perform the new item and out null calls required by the script operation.

# Process each value supplied by the selected collection.

foreach ( $outputDirectory in $outputDirectories )
{
    # Perform the new item and out null calls required by the script operation.

    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}

New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null

# Run the protected operation and route failures through the matching handler.

try
{
    # Process each value supplied by the selected collection.

    foreach ( $equationTheme in $equationThemes.GetEnumerator() )
    {
        # Process each value supplied by the selected collection.

        foreach ( $equation in $equations.GetEnumerator() )
        {
            $equationName = $equation.Key + $equationTheme.Key
            $document = @"
\documentclass[border=8pt]{standalone}
\usepackage{amsmath}
\usepackage{amssymb}
\usepackage{xcolor}
\nopagecolor
\color[HTML]{$($equationTheme.Value.ForegroundColor)}
\begin{document}
\fontsize{12pt}{14.4pt}\selectfont
\(
\displaystyle
$($equation.Value)
\)
\end{document}
"@

            # Prepare the source path, pdf path, and temporary output base path values required by the script
            # operation.

            $sourcePath = Join-Path $temporaryDirectory ( $equationName + '.tex' )
            $pdfPath = Join-Path $temporaryDirectory ( $equationName + '.pdf' )
            $temporaryOutputBasePath = Join-Path $temporaryDirectory ( $equationName + '-rendered' )
            $renderedImagePath = $temporaryOutputBasePath + '.png'

            # Perform the write all text, new, command, and out null calls required by the script operation.

            [System.IO.File]::WriteAllText(
                $sourcePath,
                $document,
                [System.Text.UTF8Encoding]::new($false)
            )

            & $latexExecutable `
                --enable-installer `
                --interaction=nonstopmode `
                --halt-on-error `
                "--output-directory=$temporaryDirectory" `
                $sourcePath | Out-Null

            # Handle the branch where lastexitcode differs from 0 or test path literal path pdf path.

            if ( $LASTEXITCODE -ne 0 -or !( Test-Path -LiteralPath $pdfPath ) )
            {
                throw "LaTeX generation failed for $equationName."
            }

            # Perform the command and out null calls required by the script operation.

            & $pdfToCairoExecutable `
                -png `
                -transp `
                -singlefile `
                -r 96 `
                $pdfPath `
                $temporaryOutputBasePath | Out-Null

            # Handle the branch where lastexitcode differs from 0 or test path literal path rendered image path.

            if (
                ( $LASTEXITCODE -ne 0 ) -or
                !( Test-Path -LiteralPath $renderedImagePath )
            )
            {
                throw "PNG rendering failed for $equationName."
            }

            # Perform the set equation image perimeter calls required by the script operation.

            Set-EquationImagePerimeter -ImagePath $renderedImagePath

            # Process each value supplied by the selected collection.

            foreach ( $outputDirectory in $outputDirectories )
            {
                # Perform the copy item calls required by the script operation.

                Copy-Item `
                    -LiteralPath $renderedImagePath `
                    -Destination ( Join-Path $outputDirectory ( $equationName + '.png' ) ) `
                    -Force
            }
        }
    }
}

# Complete the required cleanup regardless of how the protected operation finishes.

finally
{
    # Prepare the resolved temporary directory and resolved system temporary directory values required by the script
    # operation.

    $resolvedTemporaryDirectory = [System.IO.Path]::GetFullPath($temporaryDirectory)
    $resolvedSystemTemporaryDirectory = [System.IO.Path]::GetFullPath(
        [System.IO.Path]::GetTempPath()
    )

    # Handle the branch where keep temporary.

    if ( $KeepTemporary )
    {
        # Perform the write output calls required by the script operation.

        Write-Output "Retained temporary equation files in $resolvedTemporaryDirectory."
    }

    # Remove the temporary directory only when it remains below the system temporary root and carries the
    # workflow-specific safety prefix.

    elseif (
        $resolvedTemporaryDirectory.StartsWith(
            $resolvedSystemTemporaryDirectory,
            [System.StringComparison]::OrdinalIgnoreCase
        ) -and
        [System.IO.Path]::GetFileName($resolvedTemporaryDirectory).StartsWith(
            'eca-user-guide-',
            [System.StringComparison]::Ordinal
        ) -and
        ( Test-Path -LiteralPath $resolvedTemporaryDirectory )
    )
    {
        # Perform the remove item calls required by the script operation.

        Remove-Item -LiteralPath $resolvedTemporaryDirectory -Recurse -Force
    }
}

# Perform the write output calls required by the script operation.

Write-Output "Generated $($equations.Count * $equationThemes.Count) transparent User Guide equation images in:"

# Process each value supplied by the selected collection.

foreach ( $outputDirectory in $outputDirectories )
{
    # Perform the write output calls required by the script operation.

    Write-Output "  $outputDirectory"
}
