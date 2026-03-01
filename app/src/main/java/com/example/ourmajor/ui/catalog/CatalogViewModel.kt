package com.example.ourmajor.ui.catalog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ourmajor.data.catalog.Activity
import com.example.ourmajor.data.catalog.ActivityCategory
import com.example.ourmajor.data.catalog.CatalogRepository
import com.example.ourmajor.common.Result
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val categories: List<ActivityCategory> = emptyList(),
    val activities: List<Activity> = emptyList()
)

class CatalogViewModel(
    private val repo: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(CatalogUiState())
    val uiState: LiveData<CatalogUiState> = _uiState

    private var categoriesReg: ListenerRegistration? = null
    private var activitiesReg: ListenerRegistration? = null

    init {
        start()
    }

    private fun start() {
        _uiState.postValue((_uiState.value ?: CatalogUiState()).copy(isLoading = true, errorMessage = null))

        viewModelScope.launch {
            repo.ensureSeeded()
        }

        categoriesReg = repo.listenCategories { result ->
            result
                .onSuccess { list ->
                    val cur = _uiState.value ?: CatalogUiState()
                    _uiState.postValue(cur.copy(isLoading = false, categories = list, errorMessage = null))
                }
                .onFailure { e ->
                    val cur = _uiState.value ?: CatalogUiState()
                    _uiState.postValue(cur.copy(isLoading = false, errorMessage = e.message ?: "Failed to load categories"))
                }
        }

        activitiesReg = repo.listenActivities { result ->
            result
                .onSuccess { list ->
                    val cur = _uiState.value ?: CatalogUiState()
                    _uiState.postValue(cur.copy(isLoading = false, activities = list, errorMessage = null))
                }
                .onFailure { e ->
                    val cur = _uiState.value ?: CatalogUiState()
                    _uiState.postValue(cur.copy(isLoading = false, errorMessage = e.message ?: "Failed to load activities"))
                }
        }
    }

    fun activitiesForCategory(categoryId: String): List<Activity> {
        return (_uiState.value?.activities ?: emptyList()).filter { it.categoryId == categoryId }
    }

    override fun onCleared() {
        super.onCleared()
        categoriesReg?.remove()
        activitiesReg?.remove()
    }
}
