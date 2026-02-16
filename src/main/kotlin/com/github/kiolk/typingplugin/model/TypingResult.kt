package com.github.kiolk.typingplugin.model

data class TypingResult(
    var attemptNumber: Int = 0,
    var wpm: Double = 0.0,
    var errorsPerMinute: Double = 0.0,
    var accuracy: Double = 0.0,
)
