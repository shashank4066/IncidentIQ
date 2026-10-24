output "resource_group" { value = azurerm_resource_group.main.name }
output "aks_name" { value = azurerm_kubernetes_cluster.main.name }
output "registry_login_server" { value = azurerm_container_registry.main.login_server }
output "registry_name" { value = azurerm_container_registry.main.name }
output "aks_resource_id" { value = azurerm_kubernetes_cluster.main.id }
