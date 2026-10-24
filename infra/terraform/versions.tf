terraform {
  required_version = ">= 1.9, < 2.0"
  required_providers {
    azurerm = { source = "hashicorp/azurerm", version = "~> 4.0" }
  }
  # For shared use, configure an Azure Storage remote backend before applying.
}
provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}
