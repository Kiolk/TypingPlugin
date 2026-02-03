package com.github.kiolk.typingplugin.service

import com.github.kiolk.typingplugin.model.TypingResult
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "TypingStatistics",
    storages = [Storage("typingStatistics.xml")]
)
class TypingService(private val project: Project) : PersistentStateComponent<TypingService.State> {

    data class State(
        var results: MutableList<TypingResult> = mutableListOf()
    )

    private var myState = State()

    fun addResult(wpm: Double, errorsPerMinute: Double, accuracy: Double) {
        val attemptNumber = myState.results.size + 1
        myState.results.add(TypingResult(attemptNumber, wpm, errorsPerMinute, accuracy))
    }

    fun getResults(): List<TypingResult> = myState.results.toList()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): TypingService = project.service()
    }
}
