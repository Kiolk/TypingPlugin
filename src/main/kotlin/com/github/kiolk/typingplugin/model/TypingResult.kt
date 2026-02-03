package com.github.kiolk.typingplugin.model

data class TypingResult(
    val attemptNumber: Int,
    val wpm: Double,
    val errorsPerMinute: Double,
    val accuracy: Double
)
