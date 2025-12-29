# ==================================
# 1. RESOURCE GROUP
# ==================================
resource "azurerm_resource_group" "aks_rg" {
  name     = var.resource_group_name
  location = var.location
  
  tags = {
    Environment = "Dev"
    Project     = "Biblio"
    ManagedBy   = "Terraform"
  }
}

# ==================================
# 2. AZURE DATABASE FOR MYSQL
# ==================================
resource "azurerm_mysql_flexible_server" "mysql" {
  name                   = "${var.project_name}-mysql"
  resource_group_name    = azurerm_resource_group.aks_rg.name
  location               = azurerm_resource_group.aks_rg.location
  administrator_login    = var.mysql_admin_username
  administrator_password = var.mysql_admin_password
  
  sku_name = var.mysql_sku_name
  version  = "8.0.21"
  
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  
  tags = {
    Environment = "Dev"
    Project     = "Biblio"
  }
}

# Base de données MySQL
resource "azurerm_mysql_flexible_database" "database" {
  name                = var.database_name
  resource_group_name = azurerm_resource_group.aks_rg.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  charset             = "utf8mb4"
  collation           = "utf8mb4_unicode_ci"
}

# Règle de pare-feu pour autoriser tous les services Azure
resource "azurerm_mysql_flexible_server_firewall_rule" "allow_azure_services" {
  name                = "AllowAzureServices"
  resource_group_name = azurerm_resource_group.aks_rg.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  start_ip_address    = "0.0.0.0"
  end_ip_address      = "0.0.0.0"
}

resource "azurerm_mysql_flexible_server_firewall_rule" "custom_rules" {
  for_each = var.mysql_firewall_rules

  name                = each.key
  resource_group_name = azurerm_resource_group.aks_rg.name
  server_name         = azurerm_mysql_flexible_server.mysql.name
  start_ip_address    = each.value.start_ip_address
  end_ip_address      = each.value.end_ip_address
}

# ==================================
# 3. AZURE CONTAINER REGISTRY (ACR)
# ==================================
resource "azurerm_container_registry" "acr" {
  name                = "${var.project_name}acr"
  resource_group_name = azurerm_resource_group.aks_rg.name
  location            = azurerm_resource_group.aks_rg.location
  sku                 = "Basic"
  admin_enabled       = true
  
  tags = {
    Environment = "Dev"
    Project     = "Biblio"
  }
}

# ==================================
# 4. AKS CLUSTER
# ==================================
resource "azurerm_kubernetes_cluster" "aks" {
  name                = var.cluster_name
  location            = azurerm_resource_group.aks_rg.location
  resource_group_name = azurerm_resource_group.aks_rg.name
  dns_prefix          = "${var.cluster_name}-dns"
  kubernetes_version  = var.kubernetes_version
  
  default_node_pool {
    name                = "default"
    node_count          = var.node_count
    vm_size             = var.vm_size
    os_disk_size_gb     = 30
    os_disk_type        = "Managed"
    
    enable_auto_scaling = false
    type                = "VirtualMachineScaleSets"
    
    upgrade_settings {
      max_surge = "10%"
    }
  }
  
  identity {
    type = "SystemAssigned"
  }
  
  network_profile {
    network_plugin    = "kubenet"
    load_balancer_sku = "standard"
    outbound_type     = "loadBalancer"
  }
  
  sku_tier                          = "Free"
  role_based_access_control_enabled = true
  
  tags = {
    Environment = "Dev"
    Project     = "Biblio"
  }
}

# ==================================
# 5. ROLE ASSIGNMENT (AKS -> ACR)
# ==================================
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.acr.id
  skip_service_principal_aad_check = true
}

# ==================================
# 6. KUBERNETES SECRET pour MySQL
# ==================================
resource "kubernetes_secret" "mysql_connection" {
  metadata {
    name      = "mysql-connection-secret"
    namespace = "default"
  }
  
  data = {
    spring_datasource_url      = "jdbc:mysql://${azurerm_mysql_flexible_server.mysql.fqdn}:3306/${azurerm_mysql_flexible_database.database.name}?useSSL=true&requireSSL=true"
    spring_datasource_username = var.mysql_admin_username
    spring_datasource_password = var.mysql_admin_password
  }
  
  type = "Opaque"
  
  depends_on = [azurerm_kubernetes_cluster.aks]
}

# ==================================
# 7. KUBERNETES SECRET pour JWT
# ==================================
resource "kubernetes_secret" "jwt_secrets" {
  metadata {
    name      = "jwt-secrets"
    namespace = "default"
  }
  
  data = {
    jwt_secret         = var.jwt_secret
    jwt_refresh_secret = var.jwt_refresh_secret
  }
  
  type = "Opaque"
  
  depends_on = [azurerm_kubernetes_cluster.aks]
}

# ==================================
# 8. KUBERNETES SECRET pour OAuth2
# ==================================
resource "kubernetes_secret" "oauth2_secrets" {
  metadata {
    name      = "oauth2-secrets"
    namespace = "default"
  }
  
  data = {
    google_client_id     = var.google_client_id
    google_client_secret = var.google_client_secret
  }
  
  type = "Opaque"
  
  depends_on = [azurerm_kubernetes_cluster.aks]
}

# ==================================
# 9. KUBERNETES SECRET pour Email
# ==================================
resource "kubernetes_secret" "email_secrets" {
  metadata {
    name      = "email-secrets"
    namespace = "default"
  }
  
  data = {
    email_username = var.email_username
    email_password = var.email_password
  }
  
  type = "Opaque"
  
  depends_on = [azurerm_kubernetes_cluster.aks]
}

# ==================================
# 10. KUBERNETES NAMESPACE
# ==================================
resource "kubernetes_namespace" "app_namespace" {
  metadata {
    name = var.app_namespace
  }
  
  depends_on = [azurerm_kubernetes_cluster.aks]
}

# Copier tous les secrets dans le namespace de l'application
resource "kubernetes_secret" "mysql_connection_app_ns" {
  metadata {
    name      = "mysql-connection-secret"
    namespace = kubernetes_namespace.app_namespace.metadata[0].name
  }
  
  data = {
    spring_datasource_url      = "jdbc:mysql://${azurerm_mysql_flexible_server.mysql.fqdn}:3306/${azurerm_mysql_flexible_database.database.name}?useSSL=true&requireSSL=true"
    spring_datasource_username = var.mysql_admin_username
    spring_datasource_password = var.mysql_admin_password
  }
  
  type = "Opaque"
  
  depends_on = [kubernetes_namespace.app_namespace]
}

resource "kubernetes_secret" "jwt_secrets_app_ns" {
  metadata {
    name      = "jwt-secrets"
    namespace = kubernetes_namespace.app_namespace.metadata[0].name
  }
  
  data = {
    jwt_secret         = var.jwt_secret
    jwt_refresh_secret = var.jwt_refresh_secret
  }
  
  type = "Opaque"
  
  depends_on = [kubernetes_namespace.app_namespace]
}

resource "kubernetes_secret" "oauth2_secrets_app_ns" {
  metadata {
    name      = "oauth2-secrets"
    namespace = kubernetes_namespace.app_namespace.metadata[0].name
  }
  
  data = {
    google_client_id     = var.google_client_id
    google_client_secret = var.google_client_secret
  }
  
  type = "Opaque"
  
  depends_on = [kubernetes_namespace.app_namespace]
}

resource "kubernetes_secret" "email_secrets_app_ns" {
  metadata {
    name      = "email-secrets"
    namespace = kubernetes_namespace.app_namespace.metadata[0].name
  }
  
  data = {
    email_username = var.email_username
    email_password = var.email_password
  }
  
  type = "Opaque"
  
  depends_on = [kubernetes_namespace.app_namespace]
}
