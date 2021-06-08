package com.example.viewmodeleventlab

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

fun <T> LiveData<Event<T>>.observeEvent(owner: LifecycleOwner, eventHandler: (T) -> Unit) {
    this.observe(owner) {
        it.getContentIfNotHandled()?.let { content ->
            eventHandler(content)
        }
    }
}