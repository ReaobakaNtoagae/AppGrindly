package network

data class ServicePackageUpdateRequest(
    val userId: String,
    val servicePackages: List<ServicePackage>?,
    val packageStatus: String? = null
)
