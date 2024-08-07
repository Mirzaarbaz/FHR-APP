package com.example.version0

// File: DataModels.kt

data class TextViewData(
    val clock: String,
    val pname: String,
    val dilation: String,
    val result: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "clock" to clock,
            "pname" to pname,
            "dilation" to dilation,
            "result" to result
        )
    }
}
