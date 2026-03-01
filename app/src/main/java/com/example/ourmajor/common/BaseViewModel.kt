package com.example.ourmajor.common

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    protected val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    protected fun <T> execute(
        tag: String,
        loading: Boolean = true,
        block: suspend () -> Result<T>,
        onSuccess: (T) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (loading) _isLoading.postValue(true)
            _errorMessage.postValue(null)
            try {
                when (val result = block()) {
                    is Result.Success -> {
                        onSuccess(result.data)
                        Log.d(tag, "Success: ${result.data}")
                    }
                    is Result.Failure -> {
                        Log.e(tag, "Failure", result.exception)
                        val message = when {
                            result.exception.message?.contains("Unable to resolve host") == true ||
                            result.exception.message?.contains("Network is unreachable") == true -> "No internet connection"
                            result.exception.message?.contains("permission-denied") == true -> "Permission denied"
                            else -> result.exception.message ?: "Unknown error"
                        }
                        _errorMessage.postValue(message)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error", e)
                _errorMessage.postValue(e.message ?: "Unexpected error")
            } finally {
                if (loading) _isLoading.postValue(false)
            }
        }
    }

    protected fun clearError() {
        _errorMessage.postValue(null)
    }
}
