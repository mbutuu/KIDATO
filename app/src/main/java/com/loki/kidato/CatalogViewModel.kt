package com.loki.kidato

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CatalogViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadSchools() {
        db.collection("schools")
            .orderBy("order")
            .get()
            .addOnSuccessListener { snap ->
                _schools.value = snap.documents.map { d ->
                    School(
                        id = d.id,
                        name = d.getString("name") ?: "",
                        order = d.getLong("order") ?: 0
                    )
                }
            }
            .addOnFailureListener { e -> _error.value = e.message }
    }

    fun loadCourses(schoolId: String) {
        if (schoolId.isBlank()) {
            _courses.value = emptyList()
            return
        }

        db.collection("courses")
            .whereEqualTo("schoolId", schoolId)
            .get()
            .addOnSuccessListener { snap ->
                _courses.value = snap.documents.map { d ->
                    Course(
                        id = d.id,
                        name = d.getString("name") ?: "",
                        schoolId = d.getString("schoolId") ?: "",
                        order = d.getLong("order") ?: 0
                    )
                }.sortedBy { it.order }
            }
            .addOnFailureListener { e -> _error.value = e.message }
    }
}
