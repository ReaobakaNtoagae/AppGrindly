import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavouritesViewModel : ViewModel() {

    private val _favourites = MutableLiveData<List<Service>>()
    val favourites: LiveData<List<Service>> get() = _favourites

    fun loadFavourites(userToken: String, allServices: List<Service>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getFavourites("Bearer $userToken")
                if (response.isSuccessful) {
                    val favouriteIds = response.body()?.favourites ?: emptyList()
                    val favServices = allServices.filter { it.id in favouriteIds }
                        .map { it.copy(isFavourite = true) }

                    withContext(Dispatchers.Main) {
                        _favourites.value = favServices
                    }
                } else {
                    withContext(Dispatchers.Main) { _favourites.value = emptyList() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { _favourites.value = emptyList() }
            }
        }
    }
}

