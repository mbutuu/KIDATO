package com.loki.kidato

data class FileMeta(
    val id: String = "",
    val title: String = "",
    val type: String = "past_paper", // past_paper | marking_scheme
    val courseCode: String = "",
    val unitName: String = "",
    val downloadUrl: String = "",
    val uploadedByName: String = "",
    val uploadedAt: Long? = null
)
