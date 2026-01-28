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

    private val _groupedByCourse =
        MutableStateFlow<Map<String, List<FileMeta>>>(emptyMap())

    val groupedByCourse: StateFlow<Map<String, List<FileMeta>>> = _groupedByCourse


    fun startListening() {
        if (listener != null) return

        listener = db.collection("files")
            .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->

                if (e != null) {
                    _error.value = e.message
                    android.util.Log.e("FILES_VM", "Listener error", e)
                    return@addSnapshotListener
                }

                val docs = snap?.documents.orEmpty()
                android.util.Log.d("FILES_VM", "Snapshot docs = ${docs.size}")

                val list = docs.map { d ->
                    FileMeta(
                        id = d.id,
                        title = d.getString("title").orEmpty(),
                        type = d.getString("type") ?: "past_paper",
                        courseCode = d.getString("courseCode").orEmpty(),
                        unitName = d.getString("unitName").orEmpty(),
                        downloadUrl = d.getString("downloadUrl").orEmpty(),
                        uploadedByName = d.getString("uploadedByName").orEmpty(),
                        uploadedAt = d.getTimestamp("uploadedAt")?.toDate()?.time
                    )
                }

                // Update flat list
                _files.value = list

                // Update grouped list (sorted by course code, UNKNOWN last)
                val grouped = list
                    .groupBy { it.courseCode.trim().uppercase().ifBlank { "UNKNOWN" } }
                    .toList()
                    .sortedWith(compareBy<Pair<String, List<FileMeta>>> { it.first == "UNKNOWN" }.thenBy { it.first })
                    .toMap()

                _groupedByCourse.value = grouped

                // clear old errors on success
                _error.value = null

                android.util.Log.d("FILES_VM", "Updated: files=${list.size}, courses=${grouped.size}")
            }

    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
