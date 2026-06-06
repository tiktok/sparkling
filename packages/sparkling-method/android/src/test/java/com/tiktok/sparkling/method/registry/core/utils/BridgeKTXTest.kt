// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the small reflective extension helpers in BridgeKTX.kt and the
 * trivial [IAssignDir] creator. These aren't covered by other suites that
 * call [IDLMethodResultModelArguments.createModel] directly.
 */
class BridgeKTXTest {
    @Test
    fun classCreateXModelDelegatesToArguments() {
        @Suppress("DEPRECATION")
        val model = SimpleModel::class.java.createXModel()
        model.name = "alpha"

        assertEquals("alpha", model.name)
        assertNotNull(model.toString())
    }

    @Test
    fun kclassCreateXModelDelegatesToArguments() {
        @Suppress("DEPRECATION")
        val model = SimpleModel::class.createXModel()
        model.name = "beta"

        assertEquals("beta", model.name)
    }

    @Test
    fun classCreateXModelWithContainerIDPropagates() {
        val model = SimpleModel::class.java.createXModel("container-1")
        model.name = "gamma"

        assertEquals("gamma", model.name)
    }

    @Test
    fun kclassCreateXModelWithContainerIDPropagates() {
        val model = SimpleModel::class.createXModel("container-2")
        model.name = "delta"

        assertEquals("delta", model.name)
    }

    @Test
    fun assignXWrapsAnyValueIntoIAssignDir() {
        val wrapped = "payload".assignX()
        assertEquals("payload", wrapped.getValue())

        val mapWrapped = mapOf("k" to 1).assignX()

        @Suppress("UNCHECKED_CAST")
        val backing = mapWrapped.getValue() as Map<String, Int>
        assertEquals(1, backing["k"])
    }

    @Test
    fun iAssignDirCreatorPreservesIdentityForArbitraryTypes() {
        val list = listOf(1, 2, 3)
        val wrapper = IAssignDir.Creator.create(list)
        assertTrue(wrapper.getValue() === list)
    }

    private interface SimpleModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        @set:IDLMethodParamField(isGetter = false, keyPath = "name")
        var name: String?

        @get:IDLMethodParamField(
            keyPath = "count",
            isEnum = true,
            defaultValue = MethodParamDefaultValue(type = DefaultType.INT, intValue = 1),
        )
        @get:IDLMethodIntEnum(1, 2)
        @set:IDLMethodParamField(isGetter = false, keyPath = "count")
        var count: Number?
    }
}
