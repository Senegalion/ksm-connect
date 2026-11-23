param location string
param githubRepository string
param tags object

var suffix = uniqueString(resourceGroup().id)
var acrPushRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '8311e382-0749-4cb8-b61a-304f252e45ec')
var githubIssuer = 'https://token.actions.githubusercontent.com'
var githubAudience = 'api://AzureADTokenExchange'

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: 'ksmacr${suffix}'
  location: location
  tags: tags
  sku: { name: 'Basic' }
  properties: {
    adminUserEnabled: false
  }
}

resource githubBuild 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-github-build'
  location: location
  tags: tags
}

resource githubBuildCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  parent: githubBuild
  name: 'github-main'
  properties: {
    issuer: githubIssuer
    subject: 'repo:${githubRepository}:ref:refs/heads/main'
    audiences: [githubAudience]
  }
}

resource githubTest 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-github-deploy-test'
  location: location
  tags: tags
}

resource githubTestCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  parent: githubTest
  name: 'github-environment-test'
  properties: {
    issuer: githubIssuer
    subject: 'repo:${githubRepository}:environment:test'
    audiences: [githubAudience]
  }
  dependsOn: [githubBuildCredential]
}

resource githubProd 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-github-deploy-prod'
  location: location
  tags: tags
}

resource githubProdCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  parent: githubProd
  name: 'github-environment-production'
  properties: {
    issuer: githubIssuer
    subject: 'repo:${githubRepository}:environment:production'
    audiences: [githubAudience]
  }
  dependsOn: [githubTestCredential]
}

resource buildPush 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, githubBuild.id, 'acrpush')
  scope: registry
  properties: {
    principalId: githubBuild.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: acrPushRoleId
  }
}

output registryName string = registry.name
output registryLoginServer string = registry.properties.loginServer
output githubBuildClientId string = githubBuild.properties.clientId
output githubTestClientId string = githubTest.properties.clientId
output githubTestPrincipalId string = githubTest.properties.principalId
output githubProdClientId string = githubProd.properties.clientId
output githubProdPrincipalId string = githubProd.properties.principalId
