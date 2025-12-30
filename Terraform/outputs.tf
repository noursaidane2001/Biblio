# ==================================
# OUTPUTS AKS
# ==================================
output "aks_cluster_name" {
  description = "Nom du cluster AKS"
  value       = azurerm_kubernetes_cluster.aks.name
}

output "aks_cluster_id" {
  description = "ID du cluster AKS"
  value       = azurerm_kubernetes_cluster.aks.id
}

output "kube_config" {
  description = "Configuration Kubernetes (à utiliser avec kubectl)"
  value       = azurerm_kubernetes_cluster.aks.kube_config_raw
  sensitive   = true
}

output "kubernetes_version" {
  description = "Version Kubernetes utilisée"
  value       = azurerm_kubernetes_cluster.aks.kubernetes_version
}

# ==================================
# OUTPUTS MYSQL DATABASE
# ==================================
output "mysql_server_fqdn" {
  description = "FQDN du serveur MySQL"
  value       = azurerm_mysql_flexible_server.mysql.fqdn
}

output "mysql_server_name" {
  description = "Nom du serveur MySQL"
  value       = azurerm_mysql_flexible_server.mysql.name
}

output "mysql_database_name" {
  description = "Nom de la base de données"
  value       = azurerm_mysql_flexible_database.database.name
}

output "mysql_connection_string" {
  description = "Chaîne de connexion MySQL (sans le mot de passe)"
  value       = "jdbc:mysql://${azurerm_mysql_flexible_server.mysql.fqdn}:3306/${azurerm_mysql_flexible_database.database.name}"
  sensitive   = true
}

# ==================================
# OUTPUTS ACR
# ==================================
output "acr_login_server" {
  description = "URL du serveur ACR"
  value       = azurerm_container_registry.acr.login_server
}

output "acr_name" {
  description = "Nom du container registry"
  value       = azurerm_container_registry.acr.name
}

output "acr_admin_username" {
  description = "Username admin ACR"
  value       = azurerm_container_registry.acr.admin_username
  sensitive   = true
}

output "acr_admin_password" {
  description = "Mot de passe admin ACR"
  value       = azurerm_container_registry.acr.admin_password
  sensitive   = true
}

# ==================================
# OUTPUTS RESOURCE GROUP
# ==================================
output "resource_group_name" {
  description = "Nom du resource group"
  value       = azurerm_resource_group.aks_rg.name
}

output "location" {
  description = "Région Azure"
  value       = azurerm_resource_group.aks_rg.location
}

# ==================================
# COMMANDES UTILES
# ==================================
output "kubectl_config_command" {
  description = "Commande pour configurer kubectl"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.aks_rg.name} --name ${azurerm_kubernetes_cluster.aks.name}"
}

output "acr_login_command" {
  description = "Commande pour se connecter à ACR"
  value       = "az acr login --name ${azurerm_container_registry.acr.name}"
}

output "mysql_connection_command" {
  description = "Commande pour se connecter à MySQL"
  value       = "mysql -h ${azurerm_mysql_flexible_server.mysql.fqdn} -u ${var.mysql_admin_username}@${azurerm_mysql_flexible_server.mysql.name} -p"
  sensitive   = true
}

output "deployment_summary" {
  description = "Résumé du déploiement"
  value = {
    resource_group = azurerm_resource_group.aks_rg.name
    aks_cluster    = azurerm_kubernetes_cluster.aks.name
    mysql_server   = azurerm_mysql_flexible_server.mysql.name
    database       = azurerm_mysql_flexible_database.database.name
    acr            = azurerm_container_registry.acr.name
    app_namespace  = kubernetes_namespace.app_namespace.metadata[0].name
  }
}