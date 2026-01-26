package com.loki.kidato

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FilesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private val _files = MutableStateFlow<List<FileMeta>>(emptyList())
    val files: StateFlow<List<FileMeta>> = _files

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun startListening() {
        if (listener != null) return

        listener = db.collection("files")
            .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.map { d ->
                    FileMeta(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        type = d.getString("type") ?: "past_paper",
                        courseCode = d.getString("courseCode") ?: "",
                        unitName = d.getString("unitName") ?: "",
                        downloadUrl = d.getString("downloadUrl") ?: "",
                        uploadedByName = d.getString("uploadedByName") ?: "",
                        uploadedAt = d.getTimestamp("uploadedAt")?.toDate()?.time
                    )
                }
                _files.value = list
            }
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
