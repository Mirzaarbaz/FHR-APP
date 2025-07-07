package com.uptsu.fhr


// ResultListener.kt
// ResultListener.kt

interface ResultListener {
    fun onResult(numbers: List<Int>)
    fun onError(errorMessage: String)
}

