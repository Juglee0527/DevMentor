param(
    [Parameter(Mandatory = $true)]
    [string]$Model,
    [string]$BaseUrl = "http://localhost:11434",
    [string]$DatasetPath = "",
    [string]$OutputPath = "",
    [int]$TimeoutSeconds = 120,
    [int]$MaxOutputTokens = 512,
    [int]$Limit = 0,
    [string[]]$CaseId = @(),
    [switch]$Rag,
    [switch]$ShowRequest
)

$ErrorActionPreference = "Stop"

if ($MaxOutputTokens -lt 1) {
    throw "MaxOutputTokens must be at least 1."
}
if ($TimeoutSeconds -lt 1) {
    throw "TimeoutSeconds must be at least 1."
}

$repositoryRoot = Split-Path $PSScriptRoot -Parent
if ([string]::IsNullOrWhiteSpace($DatasetPath)) {
    $DatasetPath = Join-Path $repositoryRoot "docs\ai-evaluation-dataset.json"
}
$resolvedDatasetPath = (Resolve-Path -LiteralPath $DatasetPath).Path
$dataset = Get-Content -Raw -Encoding UTF8 -LiteralPath $resolvedDatasetPath | ConvertFrom-Json
$promptPath = Join-Path $PSScriptRoot "ai-evaluation-prompts.json"
$prompts = Get-Content -Raw -Encoding UTF8 -LiteralPath $promptPath | ConvertFrom-Json
$knowledgeDocuments = @()
if ($Rag) {
    $knowledgePath = Join-Path $repositoryRoot "backend\src\main\resources\knowledge\catalog.json"
    $knowledgeDocuments = @(
        Get-Content -Raw -Encoding UTF8 -LiteralPath $knowledgePath |
                ConvertFrom-Json |
                Where-Object {$_.active -eq $true -and $_.scope -eq "PUBLIC"}
    )
}

$cases = @($dataset.cases)
if ($CaseId.Count -gt 0) {
    $selectedIds = @($CaseId)
    $cases = @($cases | Where-Object { $selectedIds -contains $_.id })
    $missingIds = @($selectedIds | Where-Object { $_ -notin $cases.id })
    if ($missingIds.Count -gt 0) {
        throw "Unknown case ID: $($missingIds -join ', ')"
    }
}
if ($Limit -gt 0) {
    $cases = @($cases | Select-Object -First $Limit)
}
if ($cases.Count -eq 0) {
    throw "No evaluation cases were selected."
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $safeModelName = $Model -replace '[^a-zA-Z0-9._-]', '_'
    $resultDirectory = Join-Path $repositoryRoot "docs\ai-evaluation-results"
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputPath = Join-Path $resultDirectory "$safeModelName-$timestamp.json"
}
$outputDirectory = Split-Path $OutputPath -Parent
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}

$availableConcepts = @(
    $dataset.allowedConcepts | ForEach-Object {
        $parts = $_ -split ':', 2
        [ordered]@{
            skillCode = $parts[0]
            conceptCode = $parts[1]
            name = $parts[1]
            difficulty = "UNKNOWN"
        }
    }
)
$allowedConceptSet = @{}
foreach ($concept in $dataset.allowedConcepts) {
    $allowedConceptSet[$concept] = $true
}

function New-ObjectSchema {
    param([hashtable]$Properties, [string[]]$Required)
    return [ordered]@{
        type = "object"
        additionalProperties = $false
        properties = $Properties
        required = $Required
    }
}

function New-ConceptSchema {
    param([ValidateSet("detected", "reason")] [string]$Kind)
    $properties = [ordered]@{
        skillCode = @{type = "string"}
        conceptCode = @{type = "string"}
    }
    if ($Kind -eq "detected") {
        $properties.confidence = @{type = "number"; minimum = 0; maximum = 1}
        $required = @("skillCode", "conceptCode", "confidence")
    } else {
        $properties.reason = @{type = "string"}
        $required = @("skillCode", "conceptCode", "reason")
    }
    return New-ObjectSchema -Properties $properties -Required $required
}

function New-TutorSchema {
    return New-ObjectSchema -Properties ([ordered]@{
        answer = @{type = "string"}
        detectedConcepts = @{type = "array"; items = (New-ConceptSchema -Kind "detected")}
        knowledgeGaps = @{type = "array"; items = (New-ConceptSchema -Kind "reason")}
        followUpQuestion = @{type = @("string", "null")}
        recommendedConcepts = @{type = "array"; items = (New-ConceptSchema -Kind "reason")}
    }) -Required @(
        "answer",
        "detectedConcepts",
        "knowledgeGaps",
        "followUpQuestion",
        "recommendedConcepts"
    )
}

function New-AssessmentSchema {
    return New-ObjectSchema -Properties ([ordered]@{
        correct = @{type = "boolean"}
        score = @{type = "integer"; minimum = 0; maximum = 100}
        feedback = @{type = "string"}
        correctAnswer = @{type = "string"}
        reviewRequired = @{type = "boolean"}
    }) -Required @("correct", "score", "feedback", "correctAnswer", "reviewRequired")
}

function Test-ContainsAllTerms {
    param([string]$Text, [object[]]$Terms)
    foreach ($term in @($Terms)) {
        if ($Text.IndexOf([string]$term, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
            return $false
        }
    }
    return $true
}

function Get-P95 {
    param([long[]]$Values)
    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Max(0, [Math]::Ceiling($sorted.Count * 0.95) - 1)
    return $sorted[$index]
}

function Find-KnowledgeDocuments {
    param([string]$Question)
    if (-not $Rag) {
        return @()
    }

    $normalizedQuestion = $Question.ToLowerInvariant()
    $scored = foreach ($document in $knowledgeDocuments) {
        $score = 0
        foreach ($keyword in @($document.keywords)) {
            if ($normalizedQuestion.Contains(([string]$keyword).ToLowerInvariant())) {
                $score += 10
            }
        }
        if ($score -ge 10) {
            [pscustomobject]@{document = $document; score = $score}
        }
    }
    return @(
        $scored |
                Sort-Object @{Expression = "score"; Descending = $true},
                        @{Expression = {$_.document.id}; Descending = $false} |
                Select-Object -First 3 |
                ForEach-Object {$_.document}
    )
}

$tutorInstructions = [string]$prompts.tutorInstructions
$assessmentInstructions = [string]$prompts.assessmentInstructions

$results = [System.Collections.Generic.List[object]]::new()
$apiUrl = $BaseUrl.TrimEnd('/') + "/api/chat"
$position = 0

foreach ($case in $cases) {
    $position++
    Write-Host "[$position/$($cases.Count)] $($case.id) $($case.mode)"
    $startedAt = Get-Date
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $response = $null
    $content = $null
    $parsed = $null
    $errorType = $null
    $errorMessage = $null
    $checks = [ordered]@{}

    try {
        if ($case.mode -eq "TUTOR") {
            $retrievedDocuments = @(
                Find-KnowledgeDocuments -Question ([string]$case.request.currentQuestion) |
                        ForEach-Object {
                            [ordered]@{
                                id = $_.id
                                title = $_.title
                                content = $_.content
                                sourceUrl = $_.sourceUrl
                                version = $_.version
                            }
                        }
            )
            $requestContext = [ordered]@{
                user = $case.request.user
                currentQuestion = $case.request.currentQuestion
                recentMessages = @($case.request.recentMessages)
                conceptStatuses = @($case.request.conceptStatuses)
                availableConcepts = $availableConcepts
                retrievedDocuments = $retrievedDocuments
            }
            $input = [string]$prompts.tutorInputPrefix +
                    ($requestContext | ConvertTo-Json -Depth 15 -Compress)
            $instructions = $tutorInstructions
            $schema = New-TutorSchema
        } elseif ($case.mode -eq "ASSESSMENT") {
            $input = $case.request | ConvertTo-Json -Depth 10 -Compress
            $instructions = $assessmentInstructions
            $schema = New-AssessmentSchema
        } else {
            throw "Unsupported mode: $($case.mode)"
        }

        $body = [ordered]@{
            model = $Model
            stream = $false
            think = $false
            messages = @(
                @{role = "system"; content = $instructions},
                @{role = "user"; content = $input}
            )
            format = $schema
            options = @{
                temperature = 0
                num_predict = $MaxOutputTokens
            }
        } | ConvertTo-Json -Depth 30
        if ($ShowRequest) {
            Write-Host $body
        }
        $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)

        $httpResponse = Invoke-WebRequest `
                -UseBasicParsing `
                -Method Post `
                -Uri $apiUrl `
                -ContentType "application/json; charset=utf-8" `
                -Body $bodyBytes `
                -TimeoutSec $TimeoutSeconds
        $response = $httpResponse.Content | ConvertFrom-Json
        $content = [string]$response.message.content
        $parsed = $content | ConvertFrom-Json
        $checks.structured = $true
        $checks.koreanPresent = $content -match '[\uAC00-\uD7A3]'

        if ($case.mode -eq "TUTOR") {
            $allReturnedConcepts = @(
                @($parsed.detectedConcepts) +
                @($parsed.knowledgeGaps) +
                @($parsed.recommendedConcepts)
            )
            $invalidConcepts = @(
                $allReturnedConcepts | Where-Object {
                    -not $allowedConceptSet.ContainsKey("$($_.skillCode):$($_.conceptCode)")
                }
            )
            $expectedAllowed = @($case.expected.allowedConcepts)
            $unexpectedForCase = @(
                $allReturnedConcepts | Where-Object {
                    "$($_.skillCode):$($_.conceptCode)" -notin $expectedAllowed
                }
            )
            $checks.catalogCodesValid = $invalidConcepts.Count -eq 0
            $checks.caseCodesValid = if ($expectedAllowed.Count -eq 0) {
                $allReturnedConcepts.Count -eq 0
            } else {
                $unexpectedForCase.Count -eq 0
            }
            $checks.requiredTerms = Test-ContainsAllTerms `
                    -Text ([string]$parsed.answer) `
                    -Terms @($case.expected.requiredTerms)
            $checks.forbiddenTerms = $true
            $forbiddenTerms = @(
                $case.expected.forbiddenTerms |
                        Where-Object {
                            -not [string]::IsNullOrWhiteSpace([string]$_)
                        }
            )
            foreach ($term in $forbiddenTerms) {
                if ($content.IndexOf([string]$term, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                    $checks.forbiddenTerms = $false
                }
            }
            $checks.followUp = if ($case.expected.followUpRequired -eq $true) {
                -not [string]::IsNullOrWhiteSpace([string]$parsed.followUpQuestion)
            } else {
                $true
            }
            $checks.sourceCitation = if ($retrievedDocuments.Count -eq 0) {
                $true
            } else {
                @(
                    $retrievedDocuments | Where-Object {
                        ([string]$parsed.answer).Contains("[$($_.id)]")
                    }
                ).Count -gt 0
            }
        } else {
            $scoreRange = @($case.expected.scoreRange)
            $checks.correct = [bool]$parsed.correct -eq [bool]$case.expected.correct
            $checks.scoreRange = [int]$parsed.score -ge [int]$scoreRange[0] -and
                    [int]$parsed.score -le [int]$scoreRange[1]
            $checks.reviewRequired = [bool]$parsed.reviewRequired -eq
                    [bool]$case.expected.reviewRequired
            $checks.feedbackTerms = Test-ContainsAllTerms `
                    -Text ([string]$parsed.feedback) `
                    -Terms @($case.expected.requiredFeedbackTerms)
            $checks.logicalConsistency =
                    (([int]$parsed.score -ge 70) -eq [bool]$parsed.correct) -and
                    ([bool]$parsed.correct -or [bool]$parsed.reviewRequired)
        }
    } catch {
        $errorType = $_.Exception.GetType().Name
        $errorMessage = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $reader = [System.IO.StreamReader]::new(
                    $_.Exception.Response.GetResponseStream()
                )
                $responseError = $reader.ReadToEnd()
                $reader.Dispose()
                if (-not [string]::IsNullOrWhiteSpace($responseError)) {
                    $errorMessage = "$errorMessage Response: $responseError"
                }
            } catch {
                # Keep the original safe error when the response body cannot be read.
            }
        }
        if (-not $checks.Contains("structured")) {
            $checks.structured = $false
        }
    } finally {
        $stopwatch.Stop()
    }

    $automaticPass = $errorType -eq $null
    foreach ($check in $checks.GetEnumerator()) {
        if ($check.Value -eq $false) {
            $automaticPass = $false
        }
    }

    $results.Add([ordered]@{
        id = $case.id
        mode = $case.mode
        category = $case.category
        startedAt = $startedAt.ToString("o")
        elapsedMs = $stopwatch.ElapsedMilliseconds
        automaticPass = $automaticPass
        checks = $checks
        response = $parsed
        rawContent = $content
        metrics = if ($response) {
            [ordered]@{
                totalDurationNs = $response.total_duration
                loadDurationNs = $response.load_duration
                promptEvalCount = $response.prompt_eval_count
                evalCount = $response.eval_count
                evalDurationNs = $response.eval_duration
                doneReason = $response.done_reason
            }
        } else {
            $null
        }
        error = if ($errorType) {
            [ordered]@{type = $errorType; message = $errorMessage}
        } else {
            $null
        }
    })
}

$elapsedValues = @($results | ForEach-Object {[long]$_.elapsedMs})
$tutorElapsed = @($results | Where-Object mode -eq "TUTOR" | ForEach-Object {[long]$_.elapsedMs})
$assessmentElapsed = @(
    $results | Where-Object mode -eq "ASSESSMENT" | ForEach-Object {[long]$_.elapsedMs}
)
$successful = @($results | Where-Object {$null -eq $_.error})
$structured = @($results | Where-Object {$_.checks.structured -eq $true})
$passed = @($results | Where-Object automaticPass)

$report = [ordered]@{
    schemaVersion = 1
    evaluatedAt = (Get-Date).ToString("o")
    model = $Model
    baseUrl = $BaseUrl
    datasetPath = $resolvedDatasetPath
    datasetVersion = $dataset.datasetVersion
        settings = [ordered]@{
        timeoutSeconds = $TimeoutSeconds
        maxOutputTokens = $MaxOutputTokens
        think = $false
        temperature = 0
        promptVersion = [string]$prompts.version
        ragEnabled = [bool]$Rag
    }
    summary = [ordered]@{
        total = $results.Count
        successful = $successful.Count
        structured = $structured.Count
        automaticPass = $passed.Count
        successRate = [Math]::Round($successful.Count / $results.Count, 4)
        structuredRate = [Math]::Round($structured.Count / $results.Count, 4)
        automaticPassRate = [Math]::Round($passed.Count / $results.Count, 4)
        elapsedP95Ms = Get-P95 -Values $elapsedValues
        tutorP95Ms = Get-P95 -Values $tutorElapsed
        assessmentP95Ms = Get-P95 -Values $assessmentElapsed
    }
    results = $results
}

$report | ConvertTo-Json -Depth 40 | Set-Content -Encoding UTF8 -LiteralPath $OutputPath
Write-Host "Result: $OutputPath"
Write-Host (
    "Summary: total={0}, success={1}, structured={2}, pass={3}, p95Ms={4}" -f
    $report.summary.total,
    $report.summary.successful,
    $report.summary.structured,
    $report.summary.automaticPass,
    $report.summary.elapsedP95Ms
)
