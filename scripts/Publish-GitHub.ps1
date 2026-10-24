param(
    [ValidatePattern('^[a-zA-Z0-9_.-]+$')][string]$Repository = 'IncidentIQ',
    [switch]$Public
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'Install the official GitHub CLI first.' }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Install Git first.' }
    gh auth status
    if ($LASTEXITCODE -ne 0) {
        gh auth login --hostname github.com --git-protocol https --web
        if ($LASTEXITCODE -ne 0) { throw 'GitHub sign-in was not completed.' }
    }
    if (-not (Test-Path -LiteralPath '.git')) {
        git init -b main
        if ($LASTEXITCODE -ne 0) { throw 'Could not initialize the project repository.' }
    }
    if (-not (git config user.name) -or -not (git config user.email)) {
        throw 'Set your Git user.name and user.email in this project, then rerun.'
    }
    git add --all
    if ($LASTEXITCODE -ne 0) { throw 'Could not stage source files.' }
    git diff --cached --quiet
    if ($LASTEXITCODE -eq 1) {
        git commit -m 'Build IncidentIQ cloud incident management and RCA platform'
        if ($LASTEXITCODE -ne 0) { throw 'Could not commit source files.' }
    }
    $visibility = if ($Public) { '--public' } else { '--private' }
    gh repo create $Repository $visibility --source . --remote origin --push
    if ($LASTEXITCODE -ne 0) { throw 'Repository creation or push did not complete. Existing repositories and remotes were preserved; no force push was attempted.' }
} finally {
    Pop-Location
}
