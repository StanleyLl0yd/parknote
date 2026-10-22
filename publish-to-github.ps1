#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
$repo = 'StanleyLl0yd/parknote'
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'GitHub CLI (gh) is required.' }
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'git is required.' }
gh auth status | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Authenticate first: gh auth login' }
$existing = & gh repo view $repo --json name 2>$null
if ($LASTEXITCODE -eq 0) { throw "Repository $repo already exists. Refusing to overwrite/push automatically." }
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $root
try {
    if (Test-Path '.git') { throw 'Git repository already present; review remote and state manually.' }
    git init -b main
    if ($LASTEXITCODE -ne 0) { throw 'git init failed' }
    git add .
    git commit -m 'feat: bootstrap ParkNote Android application'
    if ($LASTEXITCODE -ne 0) { throw 'git commit failed; configure git user.name/user.email' }
    gh repo create $repo --public --source . --remote origin --push --description 'Private offline Android parking spot reminder: GPS and underground parking' --disable-wiki
    if ($LASTEXITCODE -ne 0) { throw 'GitHub repository creation or push failed' }
    $topics = @('android','kotlin','jetpack-compose','material3','offline-first','privacy','parking','rustore')
    gh repo edit $repo --add-topic ($topics -join ',')
    if ($LASTEXITCODE -ne 0) { Write-Warning 'Repository created; topics must be configured manually.' }
    gh repo view $repo --web
}
finally { Pop-Location }
