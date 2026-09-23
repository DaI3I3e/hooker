package com.example.ui.screens.scan_sms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedScanViewModel : ViewModel() {
    private val _pendingScanItem = MutableStateFlow<ScannedSmsItem?>(null)
    val pendingScanItem: StateFlow<ScannedSmsItem?> = _pendingScanItem.asStateFlow()

    fun setPendingScanItem(item: ScannedSmsItem?) {
        _pendingScanItem.value = item
        // Keep ScanSmsDataHolder in sync as secondary safety
        ScanSmsDataHolder.selectedItem = item
    }

    fun clearPendingScanItem() {
        _pendingScanItem.value = null
        ScanSmsDataHolder.selectedItem = null
    }
}
