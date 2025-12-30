# ==================================
# VARIABLES GÉNÉRALES
# ==================================
variable "resource_group_name" {
  description = "Nom du resource group"
  type        = string
  default     = "biblio-rg"
}

variable "location" {
  description = "Région Azure"
  type        = string
  default     = "France Central"
}

variable "project_name" {
  description = "Nom du projet (utilisé pour nommer les ressources)"
  type        = string
  default     = "biblio"
  
  validation {
    condition     = can(regex("^[a-z0-9]{3,20}$", var.project_name))
    error_message = "Le nom doit contenir uniquement des minuscules et chiffres, entre 3 et 20 caractères."
  }
}

# ==================================
# VARIABLES AKS
# ==================================
variable "cluster_name" {
  description = "Nom du cluster AKS"
  type        = string
  default     = "biblio-aks"
}

variable "kubernetes_version" {
  description = "Version Kubernetes"
  type        = string
  default     = "1.32.9"
}

variable "node_count" {
  description = "Nombre de nodes dans le cluster"
  type        = number
  default     = 2
}

variable "vm_size" {
  description = "Taille des VMs pour les nodes"
  type        = string
  default     = "Standard_B2s"
}

# ==================================
# VARIABLES MYSQL DATABASE
# ==================================
variable "mysql_admin_username" {
  description = "Username administrateur MySQL"
  type        = string
  sensitive   = true
}

variable "mysql_admin_password" {
  description = "Mot de passe administrateur MySQL"
  type        = string
  sensitive   = true
  
  validation {
    condition     = length(var.mysql_admin_password) >= 8
    error_message = "Le mot de passe doit contenir au moins 8 caractères."
  }
}

variable "database_name" {
  description = "Nom de la base de données"
  type        = string
  default     = "biblio1"
}

variable "mysql_sku_name" {
  description = "SKU MySQL Flexible Server"
  type        = string
  default     = "B_Standard_B1ms"
}

variable "mysql_firewall_rules" {
  description = "Règles firewall MySQL (start/end IP). Exemple: { Office = { start_ip_address = \"1.2.3.4\", end_ip_address = \"1.2.3.4\" } }"
  type = map(object({
    start_ip_address = string
    end_ip_address   = string
  }))
  default = {}
}

# ==================================
# VARIABLES JWT
# ==================================
variable "jwt_secret" {
  description = "Secret JWT pour signer les tokens"
  type        = string
  sensitive   = true
  
  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "Le secret JWT doit contenir au moins 32 caractères."
  }
}

variable "jwt_refresh_secret" {
  description = "Secret JWT pour les refresh tokens"
  type        = string
  sensitive   = true
  
  validation {
    condition     = length(var.jwt_refresh_secret) >= 32
    error_message = "Le secret JWT refresh doit contenir au moins 32 caractères."
  }
}

# ==================================
# VARIABLES OAUTH2 GOOGLE
# ==================================
variable "google_client_id" {
  description = "Google OAuth2 Client ID"
  type        = string
  sensitive   = true
}

variable "google_client_secret" {
  description = "Google OAuth2 Client Secret"
  type        = string
  sensitive   = true
}

# ==================================
# VARIABLES EMAIL
# ==================================
variable "email_username" {
  description = "Email username (Gmail)"
  type        = string
  sensitive   = true
}

variable "email_password" {
  description = "Email app password (Gmail)"
  type        = string
  sensitive   = true
}

# ==================================
# VARIABLES KUBERNETES
# ==================================
variable "app_namespace" {
  description = "Namespace Kubernetes pour l'application"
  type        = string
  default     = "biblio-app"
}
