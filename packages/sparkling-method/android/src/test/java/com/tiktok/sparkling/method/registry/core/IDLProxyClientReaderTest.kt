// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Exercises annotation selection without changing the default generated-model contract. */
class IDLProxyClientReaderTest {
    @Test
    fun customAnnotationPrefersTheMethodsOwnModel() {
        assertEquals(
            ChildMethod.ChildParams::class.java,
            IDLProxyClient.retrieveParamModel(ChildMethod::class.java, HostParamModel::class.java),
        )
        assertNull(IDLProxyClient.retrieveParamModel(ChildMethod::class.java))
    }

    @Test
    fun customAnnotationFallsBackToSuperclassWhileDefaultLookupKeepsItsOwnAnnotation() {
        assertEquals(
            ParentMethod.ParentParams::class.java,
            IDLProxyClient.retrieveParamModel(MixedMethod::class.java, HostParamModel::class.java),
        )
        assertEquals(
            MixedMethod.DefaultParams::class.java,
            IDLProxyClient.retrieveParamModel(MixedMethod::class.java),
        )
    }

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostParamModel

    abstract class ParentMethod : IDLBridgeMethod {
        @HostParamModel
        interface ParentParams : IDLMethodBaseParamModel
    }

    abstract class ChildMethod : ParentMethod() {
        @HostParamModel
        interface ChildParams : IDLMethodBaseParamModel
    }

    abstract class MixedMethod : ParentMethod() {
        @IDLMethodParamModel
        interface DefaultParams : IDLMethodBaseParamModel
    }
}
