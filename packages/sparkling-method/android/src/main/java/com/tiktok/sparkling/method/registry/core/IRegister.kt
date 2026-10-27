// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

/**
 * A flat table of bridge methods keyed by name alone.
 *
 * Unlike [IDLMethodRegistry] this carries no platform and no namespace, and it holds providers
 * rather than classes, so a registration here says how to build the method rather than which class
 * it is. A container that dispatches by name only needs nothing more than this.
 */
interface IRegister {
    /** The provider registered for [name], or null when nothing is registered. */
    fun findIDLMethod(name: String): IDLMethodProvider?

    /** Registers [methodProvider] under [name], replacing whatever was registered before. */
    fun registerMethod(
        name: String,
        methodProvider: IDLMethodProvider,
    )

    /** A snapshot of every registration held. */
    fun getIDLMethodList(): Map<String, IDLMethodProvider>
}
