targetScope = 'subscription'

@description('Azure region for all regional resources.')
param location string = 'polandcentral'

@description('GitHub repository in the OIDC subject form owner@ownerId/name@repositoryId, e.g. Senegalion@133698826/ksm-connect@1468039431. GitHub issues immutable-ID subjects, so renaming or re-creating the repository cannot reuse these credentials.')
param githubRepository string

@description('Object ID of the person running the deployment; receives Key Vault secret management rights.')
param deployerObjectId string

@description('Email address that receives operational alerts.')
param alertEmail string

@description('Deploy the application environments. Set to false for the first, registry-only deployment.')
param deployEnvironments bool = true

@description('Deploy the TEST environment. New subscriptions may allow only one Container Apps environment per region; enable once the quota allows it.')
param deployTestEnvironment bool = false

@description('Backend image reference, e.g. <registry>.azurecr.io/ksm-connect:<sha>.')
param backendImage string = ''

@description('Optional custom hostnames, e.g. test.ksm-connect.pl and ksm-connect.pl. Empty means the default Azure hostname.')
param testCustomHostname string = ''
param prodCustomHostname string = ''

@description('Sender address of the production email domain; empty uses the Azure managed domain.')
param prodEmailSenderAddress string = ''

@description('Verified customer-managed email domain linked to Communication Services, e.g. ksmconnect.pl.')
param prodEmailCustomDomain string = ''

@description('Custom hostnames bound to the PROD app with their managed certificate names: [{ hostname, certificateName }].')
param prodCustomDomains array = []

@secure()
param testPostgresPassword string = ''
@secure()
param testTokenPepper string = ''
@secure()
param testMailpitPassword string = ''
@secure()
param prodPostgresPassword string = ''
@secure()
param prodTokenPepper string = ''

var tags = { application: 'ksm-connect' }

resource sharedGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: 'rg-ksm-shared'
  location: location
  tags: union(tags, { environment: 'shared' })
}

resource testGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = if (deployTestEnvironment) {
  name: 'rg-ksm-test'
  location: location
  tags: union(tags, { environment: 'test' })
}

resource prodGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: 'rg-ksm-prod'
  location: location
  tags: union(tags, { environment: 'prod' })
}

module shared 'modules/shared.bicep' = {
  scope: sharedGroup
  name: 'ksm-shared'
  params: {
    location: location
    githubRepository: githubRepository
    tags: union(tags, { environment: 'shared' })
  }
}

module testIdentity 'modules/identity.bicep' = if (deployEnvironments && deployTestEnvironment) {
  scope: testGroup
  name: 'ksm-test-identity'
  params: {
    name: 'ksm-test-app-id'
    location: location
    tags: union(tags, { environment: 'test' })
  }
}

module prodIdentity 'modules/identity.bicep' = if (deployEnvironments) {
  scope: prodGroup
  name: 'ksm-prod-identity'
  params: {
    name: 'ksm-prod-app-id'
    location: location
    tags: union(tags, { environment: 'prod' })
  }
}

module registryAccess 'modules/registry-access.bicep' = if (deployEnvironments) {
  scope: sharedGroup
  name: 'ksm-registry-access'
  params: {
    registryName: shared.outputs.registryName
    pullPrincipalIds: [
      deployTestEnvironment ? testIdentity!.outputs.principalId : prodIdentity!.outputs.principalId
      prodIdentity!.outputs.principalId
    ]
  }
}

module test 'modules/environment.bicep' = if (deployEnvironments && deployTestEnvironment) {
  scope: testGroup
  name: 'ksm-test'
  params: {
    environmentName: 'test'
    location: location
    tags: union(tags, { environment: 'test' })
    backendImage: backendImage
    registryLoginServer: shared.outputs.registryLoginServer
    customHostname: testCustomHostname
    deployerObjectId: deployerObjectId
    alertEmail: alertEmail
    postgresPassword: testPostgresPassword
    tokenPepper: testTokenPepper
    mailpitPassword: testMailpitPassword
    emailSenderAddress: ''
    identityName: testIdentity!.outputs.name
  }
  dependsOn: [registryAccess]
}

module prod 'modules/environment.bicep' = if (deployEnvironments) {
  scope: prodGroup
  name: 'ksm-prod'
  params: {
    environmentName: 'prod'
    location: location
    tags: union(tags, { environment: 'prod' })
    backendImage: backendImage
    registryLoginServer: shared.outputs.registryLoginServer
    customHostname: prodCustomHostname
    deployerObjectId: deployerObjectId
    alertEmail: alertEmail
    postgresPassword: prodPostgresPassword
    tokenPepper: prodTokenPepper
    mailpitPassword: ''
    emailSenderAddress: prodEmailSenderAddress
    emailCustomDomain: prodEmailCustomDomain
    customDomains: prodCustomDomains
    identityName: prodIdentity!.outputs.name
  }
  dependsOn: [registryAccess]
}

module testDeployAccess 'modules/deploy-access.bicep' = if (deployTestEnvironment) {
  scope: testGroup
  name: 'ksm-test-deploy-access'
  params: {
    principalId: shared.outputs.githubTestPrincipalId
  }
}

module prodDeployAccess 'modules/deploy-access.bicep' = {
  scope: prodGroup
  name: 'ksm-prod-deploy-access'
  params: {
    principalId: shared.outputs.githubProdPrincipalId
  }
}

output registryLoginServer string = shared.outputs.registryLoginServer
output registryName string = shared.outputs.registryName
output githubBuildClientId string = shared.outputs.githubBuildClientId
output githubTestClientId string = shared.outputs.githubTestClientId
output githubProdClientId string = shared.outputs.githubProdClientId
output testAppUrl string = deployEnvironments && deployTestEnvironment ? test!.outputs.appUrl : ''
output testMailpitUrl string = deployEnvironments && deployTestEnvironment ? test!.outputs.mailpitUrl : ''
output testContainerAppName string = deployEnvironments && deployTestEnvironment ? test!.outputs.containerAppName : ''
output prodAppUrl string = deployEnvironments ? prod!.outputs.appUrl : ''
output prodContainerAppName string = deployEnvironments ? prod!.outputs.containerAppName : ''
output prodEmailManagedDomain string = deployEnvironments ? prod!.outputs.emailManagedDomain : ''
