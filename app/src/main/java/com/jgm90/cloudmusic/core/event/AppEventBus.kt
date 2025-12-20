package com.jgm90.cloudmusic.core.event

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

object AppEventBus {
    @PublishedApi
    internal val events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    @PublishedApi
    internal val stickyEvents = ConcurrentHashMap<KClass<out AppEvent>, AppEvent>()

    fun post(event: AppEvent) {
        events.tryEmit(event)
    }

    fun postSticky(event: AppEvent) {
        stickyEvents[event::class] = event
        events.tryEmit(event)
    }

    fun clearSticky(type: KClass<out AppEvent>) {
        stickyEvents.remove(type)
    }

    inline fun <reified T : AppEvent> observe(
        scope: CoroutineScope,
        receiveSticky: Boolean = true,
        crossinline onEvent: (T) -> Unit,
    ): Job {
        if (receiveSticky) {
            val sticky = stickyEvents[T::class] as? T
            if (sticky != null) {
                onEvent(sticky)
            }
        }
        return scope.launch {
            events.filterIsInstance<T>().collect { onEvent(it) }
        }
    }
}
