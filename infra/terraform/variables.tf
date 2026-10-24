variable "subscription_id" { type = string }
variable "location" {
  type = string
  default = "centralindia"
}
variable "name" {
  type = string
  default = "incidentiq"
  validation {
    condition = can(regex("^[a-z][a-z0-9-]{2,20}$", var.name))
    error_message = "Use 3–21 lowercase letters, numbers or hyphens, beginning with a letter."
  }
}
variable "registry_name" {
  type = string
  description = "Globally unique ACR name (5–50 lowercase letters and numbers)."
  validation {
    condition = can(regex("^[a-z][a-z0-9]{4,49}$", var.registry_name))
    error_message = "ACR requires 5–50 lowercase alphanumeric characters."
  }
}
variable "node_count" {
  type = number
  default = 2
  validation {
    condition = var.node_count >= 1 && var.node_count <= 6
    error_message = "Choose 1–6 nodes."
  }
}
variable "vm_size" {
  type = string
  default = "Standard_D4s_v5"
}
variable "admin_group_object_ids" {
  type = list(string)
  description = "Microsoft Entra group IDs allowed to administer AKS."
  validation {
    condition = length(var.admin_group_object_ids) > 0
    error_message = "At least one administrator group is required."
  }
}
variable "authorized_ip_ranges" {
  type = list(string)
  description = "CIDRs allowed to reach the AKS API. Include your operator IP and deployment runner egress."
  validation {
    condition = length(var.authorized_ip_ranges) > 0 && alltrue([for cidr in var.authorized_ip_ranges : can(cidrhost(cidr, 0)) && cidr != "0.0.0.0/0"])
    error_message = "Provide restricted valid CIDRs, not 0.0.0.0/0."
  }
}
