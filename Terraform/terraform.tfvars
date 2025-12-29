# ==================================
# CONFIGURATION GÉNÉRALE
# ==================================
resource_group_name = "biblio-rg"
location            = "France Central"
project_name        = "biblio"

# ==================================
# CONFIGURATION AKS
# ==================================
cluster_name       = "biblio-aks"
kubernetes_version = "1.32.9"
node_count         = 2
vm_size            = "Standard_B2s"

# ==================================
# CONFIGURATION SQL DATABASE
# ==================================
database_name = "bibliodb"

# ==================================
# CONFIGURATION KUBERNETES
# ==================================
app_namespace = "biblio-app"
