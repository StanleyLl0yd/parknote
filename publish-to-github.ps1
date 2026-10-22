#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
$repo = 'StanleyLl0yd/parknote'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

foreach ($tool in @('git', 'gh')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool is required; install it before publishing."
    }
}

& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'GitHub authentication is required: gh auth login' }

Push-Location $root
try {
    if (-not (Test-Path '.git')) {
        throw 'Git metadata is missing. Extract the complete ParkNote developer archive (including .git); do not run git init.'
    }

    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or $branch -ne 'main') { throw 'Expected the existing main branch.' }

    & git cat-file -e 'c5e15d0^{commit}'
    if ($LASTEXITCODE -ne 0) { throw 'The original ParkNote Git history is missing. Refusing to publish.' }

    $dirty = & git status --porcelain=v1
    if ($LASTEXITCODE -ne 0 -or $dirty) { throw 'Uncommitted changes found. Review and commit locally before publishing.' }

    $remote = & git remote
    if ($LASTEXITCODE -ne 0 -or $remote) { throw 'Git remotes already configured; refusing to change or overwrite them.' }

    & gh repo create $repo --public --source . --remote origin --push --description 'Offline Android utility to remember where a car is parked' --disable-wiki
    if ($LASTEXITCODE -ne 0) { throw 'GitHub repository creation/push failed; inspect gh output and git remote -v.' }

    $localSha = (& git rev-parse HEAD).Trim()
    $remoteInfo = & git ls-remote origin 'refs/heads/main'
    if ($LASTEXITCODE -ne 0 -or -not $remoteInfo) { throw 'Could not verify remote main after pushing.' }
    $remoteSha = ($remoteInfo -split '\s+')[0]
    if ($remoteSha -ne $localSha) { throw 'Remote main does not match local main.' }

    Write-Host "Verified remote main: $remoteSha"
    Write-Host "Repository: https://github.com/$repo"
    Write-Host 'GitHub Actions may still be running; check the Actions tab for build results.'
}
finally { Pop-Location }
