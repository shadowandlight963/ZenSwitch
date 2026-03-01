package com.example.ourmajor.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.ourmajor.common.BaseViewModel
import com.example.ourmajor.common.Result
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.example.ourmajor.data.activities.ActivitiesRepository
import com.example.ourmajor.data.activities.Favorite
import com.example.ourmajor.data.activities.EnhancedActivitiesRepository
import com.example.ourmajor.data.activities.MainCategory
import com.example.ourmajor.data.activities.SubActivity
import com.example.ourmajor.data.catalog.Activity
import com.example.ourmajor.data.catalog.ActivityCategory
import com.example.ourmajor.data.catalog.CatalogRepository
import com.example.ourmajor.data.progress.ActivitySession
import com.google.firebase.firestore.ListenerRegistration

data class ActivitiesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Legacy catalog data (for backward compatibility)
    val categories: List<com.example.ourmajor.data.catalog.ActivityCategory> = emptyList(),
    val activities: List<Activity> = emptyList(),
    // Enhanced activity structure
    val mainCategories: List<MainCategory> = emptyList(),
    val subActivities: List<SubActivity> = emptyList(),
    val selectedMainCategory: MainCategory? = null,
    val featuredActivities: List<SubActivity> = emptyList(),
    // User data
    val favorites: Set<String> = emptySet(),
    val recentSessions: List<ActivitySession> = emptyList()
)

class ActivitiesViewModel(
    private val repo: ActivitiesRepository = ActivitiesRepository(),
    private val catalogRepo: CatalogRepository = CatalogRepository(),
    private val enhancedRepo: EnhancedActivitiesRepository = EnhancedActivitiesRepository()
) : BaseViewModel() {

    private val _uiState = MutableLiveData(ActivitiesUiState())
    val uiState: LiveData<ActivitiesUiState> = _uiState

    private var favoritesReg: ListenerRegistration? = null
    private var recentReg: ListenerRegistration? = null
    private var categoriesReg: ListenerRegistration? = null
    private var activitiesReg: ListenerRegistration? = null

    init {
        start()
    }

    private fun start() {
        // Load enhanced activities data
        loadEnhancedActivitiesData()
        
        // Load catalog data directly (for backward compatibility)
        loadCatalogData()

        // Load favorites
        favoritesReg = repo.listenFavorites { result ->
            result
                .onSuccess { list ->
                    val favoriteIds = list.map { it.activityId }.toSet()
                    val current = _uiState.value ?: ActivitiesUiState()
                    _uiState.postValue(current.copy(favorites = favoriteIds))
                }
                .onFailure { e ->
                    _errorMessage.postValue(e.message ?: "Failed to load favorites")
                }
        }

        // Load recent sessions
        recentReg = repo.listenRecentSessions { result ->
            result
                .onSuccess { list ->
                    val current = _uiState.value ?: ActivitiesUiState()
                    _uiState.postValue(current.copy(recentSessions = list))
                }
                .onFailure { e ->
                    _errorMessage.postValue(e.message ?: "Failed to load recent sessions")
                }
        }
    }

    private fun loadCatalogData() {
        // Load categories
        categoriesReg = catalogRepo.listenCategories { result ->
            result
                .onSuccess { categories ->
                    val current = _uiState.value ?: ActivitiesUiState()
                    _uiState.postValue(current.copy(categories = categories))
                }
                .onFailure { e ->
                    _errorMessage.postValue(e.message ?: "Failed to load categories")
                }
        }

        // Load activities
        activitiesReg = catalogRepo.listenActivities { result ->
            result
                .onSuccess { activities ->
                    val current = _uiState.value ?: ActivitiesUiState()
                    _uiState.postValue(current.copy(activities = activities))
                }
                .onFailure { e ->
                    _errorMessage.postValue(e.message ?: "Failed to load activities")
                }
        }
    }

    fun toggleFavorite(activityId: String) {
        val current = _uiState.value ?: return
        val isFavorite = activityId in current.favorites

        execute<Unit>(tag = "ActivitiesViewModel", loading = false, block = {
            if (isFavorite) {
                repo.removeFavorite(activityId)
            } else {
                repo.addFavorite(activityId)
            }
        })

        // Optimistic update
        val updatedFavorites = if (isFavorite) {
            current.favorites - activityId
        } else {
            current.favorites + activityId
        }
        _uiState.postValue(current.copy(favorites = updatedFavorites))
    }

    fun startActivity(activity: Activity, minutes: Int = activity.durationMinutes) {
        execute<Unit>(tag = "ActivitiesViewModel", block = {
            repo.recordSession(activity, minutes, completed = true)
        })
    }

    fun abandonActivity(activity: Activity, minutes: Int = 1) {
        execute<Unit>(tag = "ActivitiesViewModel", block = {
            repo.recordSession(activity, minutes, completed = false)
        })
    }

    fun getHistoryForActivity(activityId: String, onResult: (Result<List<ActivitySession>>) -> Unit) {
        repo.listenActivityHistory(activityId, onResult)
    }

    // Enhanced activities methods
    private fun loadEnhancedActivitiesData() {
        val current = _uiState.value ?: ActivitiesUiState()
        val mainCategories = enhancedRepo.getMainCategories()
        val subActivities = enhancedRepo.getAllSubActivities()
        val featuredActivities = enhancedRepo.getFeaturedActivities()
        
        _uiState.postValue(current.copy(
            mainCategories = mainCategories,
            subActivities = subActivities,
            featuredActivities = featuredActivities
        ))
    }

    fun selectMainCategory(categoryId: String) {
        val current = _uiState.value ?: return
        val selectedCategory = enhancedRepo.getMainCategories().find { it.id == categoryId }
        _uiState.postValue(current.copy(selectedMainCategory = selectedCategory))
    }

    fun getSubActivitiesForCategory(categoryId: String): List<SubActivity> {
        return enhancedRepo.getSubActivitiesByCategory(categoryId)
    }

    fun getSubActivityById(id: String): SubActivity? {
        return enhancedRepo.getSubActivityById(id)
    }

    override fun onCleared() {
        super.onCleared()
        favoritesReg?.remove()
        recentReg?.remove()
        categoriesReg?.remove()
        activitiesReg?.remove()
    }
}
