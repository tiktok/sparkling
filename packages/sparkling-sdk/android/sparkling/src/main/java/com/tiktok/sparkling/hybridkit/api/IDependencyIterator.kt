// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.api

/**
 * By extends AbsDependencyIterator, a class can have iterator function, it would
 * have a next field, which point to a same class instance, therefore when you call putDependency
 * for a AbsDependencyIterator instance, it would build a linked list.
 * Besides, we provide
 * @see iterator method to execute same action for the whole list
 */
abstract class AbsDependencyIterator<T> : IDependencyIterator<T> {
    var next: T? = null

    override fun next(): T? = next

    override fun next(t: T?) {
        next = t
    }
}

interface IDependencyIterator<T> {
    fun next(): T?

    fun next(t: T?)
}

inline fun <reified T> IDependencyIterator<T>?.iterator(action: (T) -> Unit) {
    if (this == null) return
    var element = this.takeIf { it is T }?.let { it as T }
    while (element != null) {
        action(element)
        if (element is IDependencyIterator<*>) {
            element = element.next()?.takeIf { it is T }?.let { it as T }
        }
    }
}
