package com.tinyfish.jeekalarm

import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> globalStateOf(initial: T, afterSetValue: (T) -> Unit = {}): GlobalState<T> {
    return GlobalState(initial, afterSetValue)
}

class GlobalState<T>(initial: T, afterSetValue: (T) -> Unit = {}) : ReadWriteProperty<Any, T> {
    private val _state = mutableStateOf(initial)
    private val _afterSetValue: (T) -> Unit = afterSetValue

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return _state.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        _state.value = value
        _afterSetValue(value)
    }
}