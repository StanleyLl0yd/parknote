package com.sl.parknote.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sl.parknote.data.ParkingRepository
import com.sl.parknote.domain.ParkingRecord
import com.sl.parknote.location.OneShotLocation
import com.sl.parknote.photos.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParkingViewModel(private val context: Context) : ViewModel() {
    private val repository = ParkingRepository(context)
    val photos = PhotoStore(context)
    private val locator = OneShotLocation(context)

    private val _parking = MutableStateFlow<ParkingRecord?>(null)
    val parking = _parking.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _working = MutableStateFlow(false)
    val working = _working.asStateFlow()
    private val _problem = MutableStateFlow(false)
    val problem = _problem.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val loaded = repository.load()
                // A damaged record must never cause unrelated photos to be wiped.
                _parking.value = loaded
                runCatching { photos.removeUnreferenced(loaded?.photos?.toSet() ?: emptySet(), minimumAgeMillis = 24L * 60 * 60 * 1000) }
                runCatching { photos.cleanupStaleCameraFiles() }
            } catch (_: Exception) { _problem.value = true }
            finally { _ready.value = true }
        }
    }

    fun save(record: ParkingRecord, done: (Boolean) -> Unit) {
        if (!_ready.value || _working.value || _problem.value) return
        // Reserve before scheduling: two rapid taps must not queue two writes.
        _working.value = true
        viewModelScope.launch {
            try {
                record.validate()
                require(record.photos.all { photos.path(it).isFile })
                repository.save(record)
                _parking.value = record
                // A failed cleanup cannot roll back a successfully committed record.
                runCatching { photos.removeUnreferenced(record.photos.toSet()) }
                done(true)
            } catch (_: Exception) { done(false) }
            finally { _working.value = false }
        }
    }

    fun finish(done: (Boolean) -> Unit) {
        if (!_ready.value || _working.value || _problem.value) return
        _working.value = true
        viewModelScope.launch {
            try {
                repository.clear()
                _parking.value = null
                runCatching { photos.removeUnreferenced(emptySet()) }
                done(true)
            } catch (_: Exception) { done(false) }
            finally { _working.value = false }
        }
    }

    // All photo mutations share the busy state: prevent cancellation during an import.
    suspend fun importPhoto(uri: Uri): String {
        check(_ready.value && !_working.value && !_problem.value) { "Photo storage is busy" }
        _working.value = true
        return try { photos.importPhoto(uri) }
        finally { _working.value = false }
    }

    fun discardDraft(done: () -> Unit) {
        if (!_ready.value || _working.value || _problem.value) return
        _working.value = true
        viewModelScope.launch {
            try {
                photos.removeUnreferenced(_parking.value?.photos?.toSet() ?: emptySet())
            } catch (_: Exception) {
                // Best-effort cleanup; saved parking data remains intact.
            } finally {
                _working.value = false
                done()
            }
        }
    }
    suspend fun captureLocation(): OneShotLocation.Fix? = locator.capture()

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ParkingViewModel(context.applicationContext) as T
            }
        }
    }
}
