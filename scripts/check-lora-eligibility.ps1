param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$url = $BaseUrl.TrimEnd('/') + "/api/ai-feedback/training-eligibility"
$response = Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 15
$eligibility = $response.data

Write-Host (
    "Consented feedback: {0}/{1}" -f
    $eligibility.consentedFeedbackCount,
    $eligibility.minimumConsentedFeedback
)
Write-Host (
    "Corrected answers: {0}/{1}" -f
    $eligibility.correctedAnswerCount,
    $eligibility.minimumCorrectedAnswers
)

if (-not $eligibility.eligible) {
    foreach ($blocker in @($eligibility.blockers)) {
        Write-Host "BLOCKED: $blocker"
    }
    exit 2
}

Write-Host "ELIGIBLE: training data preparation may proceed."
