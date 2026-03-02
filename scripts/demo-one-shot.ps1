Param(
  [string]$OrderId = "demo-001",
  [decimal]$Amount = 50.00,
  [string]$UserId = "10",
  [string]$GatewayBaseUrl = "http://localhost:8080"
)

$body = @{
  orderId = $OrderId
  amount = $Amount
  userId = $UserId
} | ConvertTo-Json

Write-Host "Sending payment..."
Invoke-RestMethod -Method Post -Uri "$GatewayBaseUrl/payment/api/payments" -ContentType "application/json" -Body $body | Out-Host

Start-Sleep -Seconds 2

Write-Host "Jaeger UI: http://localhost:16686"
Write-Host "Grafana:   http://localhost:3000"
Write-Host "Prometheus: http://localhost:9090"
