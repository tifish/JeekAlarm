package com.tinyfish.jeekalarm.ui

import androidx.compose.MutableState
import androidx.compose.mutableStateOf

class GlobalState<T>(data: T) {
    private var stateValue: MutableState<T>? = null
    var value: T = data
        get() = stateValue?.value ?: field
        set(value) {
            field = value
            if (stateValue != null)
                stateValue!!.value = value
        }

    fun createState() {
        stateValue = mutableStateOf(value)
    }

    fun destroyState() {
        stateValue = null
    }
}
