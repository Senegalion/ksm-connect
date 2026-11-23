using './main.bicep'

param environmentName = 'prod'
param frontendBaseUrl = 'https://REPLACE-WITH-FRONTEND-DOMAIN'
param mailFrom = 'KSM Connect <DoNotReply@REPLACE-WITH-VERIFIED-SENDER-DOMAIN>'
param postgresAdminPassword = readEnvironmentVariable('POSTGRES_ADMIN_PASSWORD')
param tokenPepper = readEnvironmentVariable('TOKEN_PEPPER')
param mailUsername = readEnvironmentVariable('MAIL_USERNAME', '')
param mailPassword = readEnvironmentVariable('MAIL_PASSWORD', '')
param smsProvider = 'none'
