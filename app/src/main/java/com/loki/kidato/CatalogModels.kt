package com.loki.kidato

data class School(
    val id: String = "",
    val name: String = "",
    val order: Long = 0
)

data class Course(
    val id: String = "",
    val name: String = "",
    val schoolId: String = "",
    val order: Long = 0
)
