# PowerShell script to validate Kubernetes manifests

# Function to validate a Kubernetes manifest file
function Validate-KubernetesManifest {
    param (
        [string]$FilePath
    )
    
    Write-Host "Validating $FilePath..."
    
    try {
        # Use kubectl to validate the manifest
        $result = kubectl apply --dry-run=client -f $FilePath 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ $FilePath is valid" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $FilePath is invalid:" -ForegroundColor Red
            Write-Host $result -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ Error validating $FilePath: $_" -ForegroundColor Red
        return $false
    }
}

# Main script
Write-Host "Validating Kubernetes manifests for GrepWise..." -ForegroundColor Cyan

# Get all YAML files in the kubernetes directory
$manifestFiles = Get-ChildItem -Path . -Filter "*.yaml"

$validCount = 0
$invalidCount = 0

foreach ($file in $manifestFiles) {
    $isValid = Validate-KubernetesManifest -FilePath $file.FullName
    
    if ($isValid) {
        $validCount++
    } else {
        $invalidCount++
    }
}

# Summary
Write-Host "`nValidation Summary:" -ForegroundColor Cyan
Write-Host "Total manifests: $($manifestFiles.Count)" -ForegroundColor White
Write-Host "Valid manifests: $validCount" -ForegroundColor Green
Write-Host "Invalid manifests: $invalidCount" -ForegroundColor Red

if ($invalidCount -eq 0) {
    Write-Host "`n✅ All Kubernetes manifests are valid!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n❌ Some Kubernetes manifests are invalid. Please fix the errors and try again." -ForegroundColor Red
    exit 1
}