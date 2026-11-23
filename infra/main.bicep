targetScope = 'resourceGroup'

@description('Short environment name, e.g. dev or prod.')
@allowed(['dev', 'prod'])
param environmentName string

param location string = resourceGroup().location

@description('Public origin of the frontend, e.g. https://ksm-connect.pl')
param frontendBaseUrl string

@description('Container image reference deployed to Container Apps. The first deployment can use a public placeholder image.')
param backendImage string = 'mcr.microsoft.com/k8se/quickstart:latest'

@description('PostgreSQL administrator login.')
param postgresAdminLogin string = 'ksmadmin'

@secure()
@description('PostgreSQL administrator password. Stored in Key Vault.')
param postgresAdminPassword string

@secure()
@description('HMAC pepper for invitation, verification and reset tokens (at least 32 bytes). Stored in Key Vault.')
param tokenPepper string

@description('SMTP settings for Azure Communication Services Email or another provider.')
param mailHost string = 'smtp.azurecomm.net'
param mailPort string = '587'
param mailUsername string = ''
@secure()
param mailPassword string = ''
param mailFrom string

@allowed(['none', 'smsapi'])
param smsProvider string = 'none'
@secure()
param smsApiToken string = ''
param smsSenderName string = ''

@description('IPv4 ranges allowed to reach the backend ingress. Defaults to Cloudflare edge ranges (https://www.cloudflare.com/ips-v4).')
param allowedIngressCidrs array = [
  '173.245.48.0/20'
  '103.21.244.0/22'
  '103.22.200.0/22'
  '103.31.4.0/22'
  '141.101.64.0/18'
  '108.162.192.0/18'
  '190.93.240.0/20'
  '188.114.96.0/20'
  '197.234.240.0/22'
  '198.41.128.0/17'
  '162.158.0.0/15'
  '104.16.0.0/13'
  '104.24.0.0/14'
  '172.64.0.0/13'
  '131.0.72.0/22'
]

var prefix = 'ksm-${environmentName}'
var uniqueSuffix = uniqueString(resourceGroup().id)
var tags = {
  application: 'ksm-connect'
  environment: environmentName
}

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${prefix}-logs'
  location: location
  tags: tags
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${prefix}-ai'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id
  }
}

resource vnet 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: '${prefix}-vnet'
  location: location
  tags: tags
  properties: {
    addressSpace: { addressPrefixes: ['10.40.0.0/16'] }
    subnets: [
      {
        name: 'container-apps'
        properties: {
          addressPrefix: '10.40.0.0/23'
          delegations: [
            { name: 'aca', properties: { serviceName: 'Microsoft.App/environments' } }
          ]
        }
      }
      {
        name: 'postgres'
        properties: {
          addressPrefix: '10.40.2.0/24'
          delegations: [
            { name: 'pg', properties: { serviceName: 'Microsoft.DBforPostgreSQL/flexibleServers' } }
          ]
        }
      }
    ]
  }
}

resource postgresDnsZone 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: '${prefix}.private.postgres.database.azure.com'
  location: 'global'
  tags: tags
}

resource postgresDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: postgresDnsZone
  name: '${prefix}-vnet-link'
  location: 'global'
  properties: {
    registrationEnabled: false
    virtualNetwork: { id: vnet.id }
  }
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: '${prefix}-pg-${uniqueSuffix}'
  location: location
  tags: tags
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '17'
    administratorLogin: postgresAdminLogin
    administratorLoginPassword: postgresAdminPassword
    storage: {
      storageSizeGB: 32
      autoGrow: 'Enabled'
    }
    backup: {
      backupRetentionDays: environmentName == 'prod' ? 14 : 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: { mode: 'Disabled' }
    network: {
      delegatedSubnetResourceId: vnet.properties.subnets[1].id
      privateDnsZoneArmResourceId: postgresDnsZone.id
      publicNetworkAccess: 'Disabled'
    }
  }
  dependsOn: [postgresDnsLink]
}

resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: postgres
  name: 'ksm_connect'
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

resource requireTls 'Microsoft.DBforPostgreSQL/flexibleServers/configurations@2024-08-01' = {
  parent: postgres
  name: 'require_secure_transport'
  properties: {
    value: 'on'
    source: 'user-override'
  }
}

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: replace('${prefix}acr${uniqueSuffix}', '-', '')
  location: location
  tags: tags
  sku: { name: 'Basic' }
  properties: {
    adminUserEnabled: false
  }
}

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: '${prefix}-backend-id'
  location: location
  tags: tags
}

var acrPullRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
var keyVaultSecretsUserRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')

resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, identity.id, 'acrpull')
  scope: registry
  properties: {
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: acrPullRoleId
  }
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: take('${prefix}-kv-${uniqueSuffix}', 24)
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 30
    enablePurgeProtection: true
    publicNetworkAccess: 'Enabled'
  }
}

resource kvSecretsUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, identity.id, 'secrets-user')
  scope: keyVault
  properties: {
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: keyVaultSecretsUserRoleId
  }
}

var secretValues = {
  'database-password': postgresAdminPassword
  'token-pepper': tokenPepper
  'mail-password': empty(mailPassword) ? 'unset' : mailPassword
  'smsapi-token': empty(smsApiToken) ? 'unset' : smsApiToken
}

resource secrets 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = [for secret in items(secretValues): {
  parent: keyVault
  name: secret.key
  properties: { value: secret.value }
}]

resource containerEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: '${prefix}-env'
  location: location
  tags: tags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    vnetConfiguration: {
      infrastructureSubnetId: vnet.properties.subnets[0].id
      internal: false
    }
    workloadProfiles: [
      { name: 'Consumption', workloadProfileType: 'Consumption' }
    ]
  }
}

var keyVaultSecretUrl = '${keyVault.properties.vaultUri}secrets/'

resource backend 'Microsoft.App/containerApps@2024-03-01' = {
  name: '${prefix}-api'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: { '${identity.id}': {} }
  }
  properties: {
    environmentId: containerEnvironment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
        ipSecurityRestrictions: [for (cidr, index) in allowedIngressCidrs: {
          name: 'cloudflare-${index}'
          ipAddressRange: cidr
          action: 'Allow'
        }]
      }
      registries: [
        { server: registry.properties.loginServer, identity: identity.id }
      ]
      secrets: [for secret in items(secretValues): {
        name: secret.key
        keyVaultUrl: '${keyVaultSecretUrl}${secret.key}'
        identity: identity.id
      }]
    }
    template: {
      scale: {
        minReplicas: 1
        maxReplicas: 1
      }
      containers: [
        {
          name: 'backend'
          image: backendImage
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          env: [
            { name: 'SPRING_PROFILES_ACTIVE', value: 'prod' }
            { name: 'DATABASE_URL', value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/ksm_connect?sslmode=require' }
            { name: 'DATABASE_USERNAME', value: postgresAdminLogin }
            { name: 'DATABASE_PASSWORD', secretRef: 'database-password' }
            { name: 'DATABASE_POOL_SIZE', value: '8' }
            { name: 'TOKEN_PEPPER', secretRef: 'token-pepper' }
            { name: 'FRONTEND_BASE_URL', value: frontendBaseUrl }
            { name: 'CORS_ALLOWED_ORIGINS', value: frontendBaseUrl }
            { name: 'MAIL_HOST', value: mailHost }
            { name: 'MAIL_PORT', value: mailPort }
            { name: 'MAIL_USERNAME', value: mailUsername }
            { name: 'MAIL_PASSWORD', secretRef: 'mail-password' }
            { name: 'MAIL_FROM', value: mailFrom }
            { name: 'SMS_PROVIDER', value: smsProvider }
            { name: 'SMSAPI_TOKEN', secretRef: 'smsapi-token' }
            { name: 'SMS_SENDER_NAME', value: smsSenderName }
            { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: appInsights.properties.ConnectionString }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: { path: '/actuator/health/liveness', port: 8080 }
              initialDelaySeconds: 30
              periodSeconds: 20
            }
            {
              type: 'Readiness'
              httpGet: { path: '/actuator/health/readiness', port: 8080 }
              initialDelaySeconds: 15
              periodSeconds: 10
            }
          ]
        }
      ]
    }
  }
  dependsOn: [acrPull, kvSecretsUser, secrets]
}

resource frontend 'Microsoft.Web/staticSites@2023-12-01' = {
  name: '${prefix}-web'
  location: 'westeurope'
  tags: tags
  sku: { name: 'Free', tier: 'Free' }
  properties: {}
}

resource backendRestartAlert 'Microsoft.Insights/metricAlerts@2018-03-01' = {
  name: '${prefix}-api-restarts'
  location: 'global'
  tags: tags
  properties: {
    severity: 2
    enabled: true
    scopes: [backend.id]
    evaluationFrequency: 'PT5M'
    windowSize: 'PT15M'
    criteria: {
      'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
      allOf: [
        {
          name: 'restarts'
          metricName: 'RestartCount'
          operator: 'GreaterThan'
          threshold: 2
          timeAggregation: 'Total'
          criterionType: 'StaticThresholdCriterion'
        }
      ]
    }
  }
}

output registryLoginServer string = registry.properties.loginServer
output backendFqdn string = backend.properties.configuration.ingress.fqdn
output containerAppName string = backend.name
output staticWebAppName string = frontend.name
output keyVaultName string = keyVault.name
output postgresFqdn string = postgres.properties.fullyQualifiedDomainName
