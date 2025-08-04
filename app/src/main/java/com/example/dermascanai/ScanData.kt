package com.example.dermascanai

data class  ScanData(
    val condition: String? = "",
    val imageBase64: String? = "",
    val remedy: String? = "",
    val timestamp: String? = ""
)
