package com.dailyworktracker.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Factory for AddEditActivityViewModel that injects activityId into SavedStateHandle.
 * Used because we need to pass a non-primitive argument to the ViewModel.
 */
class AddEditViewModelFactory(
    private val activityId: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // We can't inject SavedStateHandle via this simple factory,
        // so we handle activityId directly in the ViewModel init.
        throw UnsupportedOperationException("Use createWithDefaultCreationExtras")
    }
}

/**
 * Simple factory that passes the activityId as a default arg.
 * Compose's viewModel() will use AndroidViewModel's default factory,
 * and we pass activityId through the CreationExtras/Bundle route.
 *
 * In practice with Navigation Compose, activityId comes from the nav backstack,
 * so we use a direct constructor factory approach.
 */
fun addEditViewModelFactory(
    application: Application,
    activityId: String?
) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val handle = SavedStateHandle(buildMap {
            activityId?.let { put("activityId", it) }
        })
        return AddEditActivityViewModel(application, handle) as T
    }
}
