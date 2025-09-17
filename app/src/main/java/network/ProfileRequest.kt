package network

data class ProfileRequest(
    val userId: String,
    val title: String,
    val category: String,
    val location: String,
    val price: String,
    val description: String,
    val profilePictureURL: String,
    val workImageURLs: List<String>,
    val documentURLs: List<String>,
    val verifiedBadgeTier: String,
    val servicePackages: List<ServicePackage>?,
    val packageStatus: String? = null
)