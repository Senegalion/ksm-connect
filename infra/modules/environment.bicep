@allowed(['test', 'prod'])
param environmentName string
param location string
param tags object
param backendImage string
param registryLoginServer string
param customHostname string
param deployerObjectId string
param alertEmail string
param emailSenderAddress string
param identityName string

@secure()
param postgresPassword string
@secure()
param tokenPepper string
@secure()
param mailpitPassword string

var isProd = environmentName == 'prod'
var prefix = 'ksm-${environmentName}'
var suffix = uniqueString(resourceGroup().id)
var appName = '${prefix}-app'
var mailpitName = '${prefix}-mailpit'
var postgresAdmin = 'ksmadmin'
var keyVaultSecretsUserRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
var keyVaultSecretsOfficerRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b86a8fe4-44ce-4948-aee5-eccb2c155cd7')
var contributorRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b24988ac-6180-42a0-ab88-20f7382dd24c')

resource logs 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${prefix}-logs'
  location: location
  tags: tags
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
    workspaceCapping: { dailyQuotaGb: isProd ? 1 : json('0.2') }
  }
}

resource insights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${prefix}-insights'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logs.id
    IngestionMode: 'LogAnalytics'
  }
}

resource network 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: '${prefix}-vnet'
  location: location
  tags: tags
  properties: {
    addressSpace: { addressPrefixes: ['10.50.0.0/16'] }
    subnets: [
      {
        name: 'container-apps'
        properties: {
          addressPrefix: '10.50.0.0/23'
          delegations: [{ name: 'aca', properties: { serviceName: 'Microsoft.App/environments' } }]
        }
      }
      {
        name: 'postgres'
        properties: {
          addressPrefix: '10.50.2.0/24'
          delegations: [{ name: 'pg', properties: { serviceName: 'Microsoft.DBforPostgreSQL/flexibleServers' } }]
        }
      }
    ]
  }
}

resource postgresDns 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: '${prefix}.private.postgres.database.azure.com'
  location: 'global'
  tags: tags
}

resource postgresDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: postgresDns
  name: '${prefix}-vnet'
  location: 'global'
  properties: {
    registrationEnabled: false
    virtualNetwork: { id: network.id }
  }
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: '${prefix}-pg-${suffix}'
  location: location
  tags: tags
  sku: { name: 'Standard_B1ms', tier: 'Burstable' }
  properties: {
    version: '17'
    administratorLogin: postgresAdmin
    administratorLoginPassword: postgresPassword
    storage: { storageSizeGB: 32, autoGrow: 'Enabled' }
    backup: { backupRetentionDays: isProd ? 14 : 7, geoRedundantBackup: 'Disabled' }
    highAvailability: { mode: 'Disabled' }
    network: {
      delegatedSubnetResourceId: network.properties.subnets[1].id
      privateDnsZoneArmResourceId: postgresDns.id
      publicNetworkAccess: 'Disabled'
    }
  }
  dependsOn: [postgresDnsLink]
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: postgres
  name: 'ksm_connect'
  properties: { charset: 'UTF8', collation: 'en_US.utf8' }
}

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' existing = {
  name: identityName
}

resource vault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: take('${prefix}-kv-${suffix}', 24)
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: isProd ? 90 : 7
    enablePurgeProtection: isProd ? true : null
  }
}

resource appSecretsAccess 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(vault.id, identity.id, 'secrets-user')
  scope: vault
  properties: {
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: keyVaultSecretsUserRoleId
  }
}

resource deployerSecretsAccess 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(vault.id, deployerObjectId, 'secrets-officer')
  scope: vault
  properties: {
    principalId: deployerObjectId
    principalType: 'User'
    roleDefinitionId: keyVaultSecretsOfficerRoleId
  }
}

var baseSecrets = {
  'database-password': postgresPassword
  'token-pepper': tokenPepper
}
var secretValues = isProd ? baseSecrets : union(baseSecrets, { 'mailpit-ui-auth': 'ksm:${mailpitPassword}' })

resource secrets 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = [for secret in items(secretValues): {
  parent: vault
  name: secret.key
  properties: { value: secret.value }
}]

resource emailService 'Microsoft.Communication/emailServices@2023-04-01' = if (isProd) {
  name: '${prefix}-email-${suffix}'
  location: 'global'
  tags: tags
  properties: { dataLocation: 'Europe' }
}

resource managedEmailDomain 'Microsoft.Communication/emailServices/domains@2023-04-01' = if (isProd) {
  parent: emailService
  name: 'AzureManagedDomain'
  location: 'global'
  tags: tags
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource communication 'Microsoft.Communication/communicationServices@2023-04-01' = if (isProd) {
  name: '${prefix}-acs-${suffix}'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: 'Europe'
    linkedDomains: [managedEmailDomain.id]
  }
}

resource communicationAccess 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (isProd) {
  name: guid(resourceGroup().id, identity.id, 'acs-contributor')
  scope: communication
  properties: {
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: contributorRoleId
  }
}

resource environment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: '${prefix}-env'
  location: location
  tags: tags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logs.properties.customerId
        sharedKey: logs.listKeys().primarySharedKey
      }
    }
    vnetConfiguration: {
      infrastructureSubnetId: network.properties.subnets[0].id
      internal: false
    }
    workloadProfiles: [{ name: 'Consumption', workloadProfileType: 'Consumption' }]
  }
}

resource mailpit 'Microsoft.App/containerApps@2024-03-01' = if (!isProd) {
  name: mailpitName
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: { '${identity.id}': {} }
  }
  properties: {
    environmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 8025
        transport: 'http'
        allowInsecure: false
        additionalPortMappings: [{ external: false, targetPort: 1025, exposedPort: 1025 }]
      }
      secrets: [{ name: 'mailpit-ui-auth', keyVaultUrl: '${vault.properties.vaultUri}secrets/mailpit-ui-auth', identity: identity.id }]
    }
    template: {
      scale: { minReplicas: 1, maxReplicas: 1 }
      containers: [
        {
          name: 'mailpit'
          image: 'docker.io/axllent/mailpit:v1.34'
          resources: { cpu: json('0.25'), memory: '0.5Gi' }
          env: [
            { name: 'MP_UI_AUTH', secretRef: 'mailpit-ui-auth' }
            { name: 'MP_MAX_MESSAGES', value: '2000' }
          ]
        }
      ]
    }
  }
  dependsOn: [appSecretsAccess, secrets]
}

var defaultAppUrl = 'https://${appName}.${environment.properties.defaultDomain}'
var appUrl = empty(customHostname) ? defaultAppUrl : 'https://${customHostname}'
var managedSenderAddress = isProd ? 'DoNotReply@${managedEmailDomain!.properties.mailFromSenderDomain}' : ''

var mailSettings = isProd ? [
  { name: 'EMAIL_PROVIDER', value: 'acs' }
  { name: 'ACS_ENDPOINT', value: 'https://${communication!.properties.hostName}' }
  { name: 'ACS_SENDER_ADDRESS', value: empty(emailSenderAddress) ? managedSenderAddress : emailSenderAddress }
] : [
  { name: 'EMAIL_PROVIDER', value: 'smtp' }
  { name: 'MAIL_HOST', value: mailpitName }
  { name: 'MAIL_PORT', value: '1025' }
  { name: 'MAIL_SMTP_AUTH', value: 'false' }
  { name: 'MAIL_STARTTLS', value: 'false' }
  { name: 'MAIL_FROM', value: 'KSM Connect TEST <no-reply@test.ksm.invalid>' }
]

resource app 'Microsoft.App/containerApps@2024-03-01' = {
  name: appName
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: { '${identity.id}': {} }
  }
  properties: {
    environmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      maxInactiveRevisions: 5
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
      }
      registries: [{ server: registryLoginServer, identity: identity.id }]
      secrets: [
        { name: 'database-password', keyVaultUrl: '${vault.properties.vaultUri}secrets/database-password', identity: identity.id }
        { name: 'token-pepper', keyVaultUrl: '${vault.properties.vaultUri}secrets/token-pepper', identity: identity.id }
      ]
    }
    template: {
      scale: { minReplicas: isProd ? 1 : 0, maxReplicas: 1 }
      containers: [
        {
          name: 'app'
          image: backendImage
          resources: { cpu: json('0.5'), memory: '1Gi' }
          env: concat([
            { name: 'SPRING_PROFILES_ACTIVE', value: 'prod' }
            { name: 'KSM_ENVIRONMENT', value: environmentName }
            { name: 'DATABASE_URL', value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/ksm_connect?sslmode=require' }
            { name: 'DATABASE_USERNAME', value: postgresAdmin }
            { name: 'DATABASE_PASSWORD', secretRef: 'database-password' }
            { name: 'DATABASE_POOL_SIZE', value: '5' }
            { name: 'TOKEN_PEPPER', secretRef: 'token-pepper' }
            { name: 'FRONTEND_BASE_URL', value: appUrl }
            { name: 'CORS_ALLOWED_ORIGINS', value: appUrl }
            { name: 'API_DOCS_ENABLED', value: isProd ? 'false' : 'true' }
            { name: 'SMS_PROVIDER', value: 'none' }
            { name: 'AZURE_CLIENT_ID', value: identity.properties.clientId }
            { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: insights.properties.ConnectionString }
            { name: 'APPLICATIONINSIGHTS_ROLE_NAME', value: appName }
            { name: 'JAVA_TOOL_OPTIONS', value: '-javaagent:/app/agent/applicationinsights-agent.jar -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError' }
          ], mailSettings)
          probes: [
            {
              type: 'Startup'
              httpGet: { path: '/actuator/health/liveness', port: 8080 }
              initialDelaySeconds: 10
              periodSeconds: 5
              failureThreshold: 36
            }
            {
              type: 'Liveness'
              httpGet: { path: '/actuator/health/liveness', port: 8080 }
              periodSeconds: 20
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: { path: '/actuator/health/readiness', port: 8080 }
              periodSeconds: 10
              failureThreshold: 3
            }
          ]
        }
      ]
    }
  }
  dependsOn: [appSecretsAccess, secrets, database]
}

resource alerts 'Microsoft.Insights/actionGroups@2023-01-01' = if (isProd) {
  name: '${prefix}-alerts'
  location: 'global'
  tags: tags
  properties: {
    groupShortName: 'ksmprod'
    enabled: true
    emailReceivers: [{ name: 'owner', emailAddress: alertEmail, useCommonAlertSchema: true }]
  }
}

var metricAlerts = [
  { name: 'app-restarts', scope: app.id, metric: 'RestartCount', operator: 'GreaterThan', threshold: 2, aggregation: 'Total', dimensions: [], severity: 2 }
  { name: 'app-server-errors', scope: app.id, metric: 'Requests', operator: 'GreaterThan', threshold: 10, aggregation: 'Total', dimensions: [{ name: 'statusCodeCategory', operator: 'Include', values: ['5xx'] }], severity: 2 }
  { name: 'db-cpu', scope: postgres.id, metric: 'cpu_percent', operator: 'GreaterThan', threshold: 85, aggregation: 'Average', dimensions: [], severity: 3 }
  { name: 'db-storage', scope: postgres.id, metric: 'storage_percent', operator: 'GreaterThan', threshold: 80, aggregation: 'Average', dimensions: [], severity: 2 }
]

resource metricAlert 'Microsoft.Insights/metricAlerts@2018-03-01' = [for alert in metricAlerts: if (isProd) {
  name: '${prefix}-${alert.name}'
  location: 'global'
  tags: tags
  properties: {
    severity: alert.severity
    enabled: true
    scopes: [alert.scope]
    evaluationFrequency: 'PT5M'
    windowSize: 'PT15M'
    criteria: {
      'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
      allOf: [
        {
          name: alert.name
          criterionType: 'StaticThresholdCriterion'
          metricName: alert.metric
          operator: alert.operator
          threshold: alert.threshold
          timeAggregation: alert.aggregation
          dimensions: alert.dimensions
        }
      ]
    }
    actions: [{ actionGroupId: alerts.id }]
  }
}]

resource availability 'Microsoft.Insights/webtests@2022-06-15' = if (isProd) {
  name: '${prefix}-availability'
  location: location
  tags: union(tags, { 'hidden-link:${insights.id}': 'Resource' })
  kind: 'standard'
  properties: {
    SyntheticMonitorId: '${prefix}-availability'
    Name: 'KSM Connect readiness'
    Enabled: true
    Frequency: 300
    Timeout: 30
    Kind: 'standard'
    RetryEnabled: true
    Locations: [{ Id: 'emea-nl-ams-azr' }, { Id: 'emea-gb-db3-azr' }, { Id: 'emea-fr-pra-edge' }]
    Request: {
      RequestUrl: '${appUrl}/actuator/health/readiness'
      HttpVerb: 'GET'
      ParseDependentRequests: false
    }
    ValidationRules: { ExpectedHttpStatusCode: 200, SSLCheck: true, SSLCertRemainingLifetimeCheck: 7 }
  }
}

resource availabilityAlert 'Microsoft.Insights/metricAlerts@2018-03-01' = if (isProd) {
  name: '${prefix}-availability-alert'
  location: 'global'
  tags: tags
  properties: {
    severity: 1
    enabled: true
    scopes: [availability.id, insights.id]
    evaluationFrequency: 'PT5M'
    windowSize: 'PT5M'
    criteria: {
      'odata.type': 'Microsoft.Azure.Monitor.WebtestLocationAvailabilityCriteria'
      webTestId: availability.id
      componentId: insights.id
      failedLocationCount: 2
    }
    actions: [{ actionGroupId: alerts.id }]
  }
}

output appUrl string = appUrl
output containerAppName string = app.name
output mailpitUrl string = isProd ? '' : 'https://${mailpit!.properties.configuration.ingress.fqdn}'
output emailManagedDomain string = isProd ? managedEmailDomain!.properties.mailFromSenderDomain : ''
