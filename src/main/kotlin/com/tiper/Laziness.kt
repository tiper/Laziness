package com.tiper

import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("ClassName")
internal object UNINITIALIZED_VALUE

/**
 * Represents a value with lazy initialization.
 *
 * To create an instance of [Laziness] use the [laziness] function.
 */
interface Laziness<in R, out T> : ReadOnlyProperty<R, T>

/**
 * Specifies how a [Laziness] instance synchronizes initialization among multiple threads.
 */
enum class LazinessThreadSafetyMode {

    /**
     * Locks are used to ensure that only a single thread can initialize the [Laziness] instance.
     */
    SYNCHRONIZED,

    /**
     * No locks are used to synchronize an access to the [Laziness] instance value; if the instance is accessed from multiple threads, its behavior is undefined.
     *
     * This mode should not be used unless the [Laziness] instance is guaranteed never to be initialized from more than one thread.
     */
    NONE
}

/**
 * Specifies how a [Laziness] instance should implement mappings.
 */
enum class LazinessMemoryMode {

    /**
     * Locks are used to ensure that only a single thread can initialize.
     * Initializer function can be called several times on concurrent access but only the first returned value will be used.
     */
    SINGLETON,

    /**
     * Prevent value referents from being made finalizable, finalized, and then reclaimed.
     */
    STRONG,

    /**
     * Do not prevent referents from being made finalizable, finalized, and then reclaimed.
     */
    WEAK
}

/**
 * Creates a new instance of the [Laziness] that uses the specified initialization function [initializer]
 * and thread-safety [thread]. Default thread-safety mode is [LazinessThreadSafetyMode.SYNCHRONIZED].
 *
 * Note that the returned instance uses itself to synchronize on.
 * Do not synchronize from external code on the returned instance as it may cause accidental deadlock.
 */
fun <R , T> laziness(
    thread: LazinessThreadSafetyMode = LazinessThreadSafetyMode.SYNCHRONIZED,
    memory: LazinessMemoryMode = LazinessMemoryMode.STRONG,
    initializer: R.() -> T
): Laziness<R, T> =
    when (memory) {
        LazinessMemoryMode.SINGLETON -> SingletonLazinessImpl(initializer)
        LazinessMemoryMode.STRONG -> when (thread) {
            LazinessThreadSafetyMode.SYNCHRONIZED -> SynchronizedLazinessImpl(initializer)
            LazinessThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
        }
        LazinessMemoryMode.WEAK -> when (thread) {
            LazinessThreadSafetyMode.SYNCHRONIZED -> WeakSynchronizedLazinessImpl(initializer)
            LazinessThreadSafetyMode.NONE -> WeakUnsafeLazyImpl(initializer)
        }
    }

private class SingletonLazinessImpl<in R, out T>(initializer: R.() -> T) : Laziness<R, T> {
    private var initializer: (R.() -> T)? = initializer
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val v1 = _value
        if (v1 !== UNINITIALIZED_VALUE) {
            return v1 as T
        }
        return synchronized(this) {
            val v2 = _value
            if (v2 != UNINITIALIZED_VALUE) {
                v2 as T
            } else {
                _value = initializer!!(thisRef)
                initializer = null
                _value as T
            }
        }
    }
}

private class SynchronizedLazinessImpl<in R, out T>(private val initializer: R.() -> T) :
    Laziness<R, T> {
    private val values = Collections.synchronizedMap(WeakHashMap<R, Any?>())

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val v1 = values.getOrElse(thisRef) { UNINITIALIZED_VALUE }
        if (v1 !== UNINITIALIZED_VALUE) {
            return v1 as T
        }
        return synchronized(values) {
            values.getOrPut(thisRef) { initializer(thisRef) } as T
        }
    }
}

private class UnsafeLazyImpl<in R, out T>(private val initializer: R.() -> T) : Laziness<R, T> {
    private val values = WeakHashMap<R, Any?>()

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val v1 = values.getOrElse(thisRef) { UNINITIALIZED_VALUE }
        if (v1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return v1 as T
        }
        return initializer(thisRef).also { values[thisRef] = it }
    }
}

private class WeakSynchronizedLazinessImpl<in R, out T>(private val initializer: R.() -> T) :
    Laziness<R, T> {
    private val _value: WeakReference<Any?> = WeakReference(UNINITIALIZED_VALUE)
    private val values = Collections.synchronizedMap(WeakHashMap<R, WeakReference<Any?>>())

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val v1 = values.getOrElse(thisRef) { _value }.get()
        if (v1 !== UNINITIALIZED_VALUE) {
            return v1 as T
        }
        return synchronized(values) {
            values.getOrPut(thisRef) {
                WeakReference(initializer(thisRef))
            }.get() as T
        }
    }
}

private class WeakUnsafeLazyImpl<in R, out T>(private val initializer: R.() -> T) : Laziness<R, T> {
    private val _value: WeakReference<Any?> = WeakReference(UNINITIALIZED_VALUE)
    private val values = WeakHashMap<R, WeakReference<Any?>>()

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val v1 =
            values.getOrElse(thisRef) { _value }.get()
        if (v1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return v1 as T
        }
        return initializer(thisRef).also { values[thisRef] = WeakReference<Any?>(it) }
    }
}
