package com.tiktok.sparkling.hybridkit

import com.tiktok.sparkling.hybridkit.api.AbsDependencyIterator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridEnvironmentTest {
    @Test
    fun keepDependencyRulesWorkForSingleAndBatchRegistration() {
        val env = HybridEnvironment()
        env.addKeepDependencyWhenRecreate(String::class.java)
        env.addKeepDependencyWhenRecreate(listOf(Int::class.javaObjectType))

        assertTrue(env.shouldKeepDependencyWhenRecreate("v"))
        assertTrue(env.shouldKeepDependencyWhenRecreate(1))
        assertFalse(env.shouldKeepDependencyWhenRecreate(1L))
    }

    @Test
    fun putGetRemoveDependencyForNormalInstance() {
        val env = HybridEnvironment()
        env.putDependency("c1", String::class.java, "hello")

        assertEquals("hello", env.getDependency("c1", String::class.java))

        env.removeDependency("c1", String::class.java)
        assertNull(env.getDependency("c1", String::class.java))
    }

    @Test
    fun removeDependencyRemovesSpecificIteratorFromChain() {
        val env = HybridEnvironment()
        val first = Node("first")
        val second = Node("second")
        val third = Node("third")

        env.putDependency("c2", Node::class.java, first)
        env.putDependency("c2", Node::class.java, second)
        env.putDependency("c2", Node::class.java, third)

        env.removeDependency("c2", Node::class.java, second)

        val head = env.getDependency("c2", Node::class.java)
        assertNotNull(head)
        assertEquals("first", head?.name)
        assertEquals("third", head?.next()?.name)
    }

    @Test
    fun cloneDependencyCreatesIndependentContainerProvider() {
        val env = HybridEnvironment()
        env.putDependency("old", String::class.java, "value")

        env.cloneDependency("old", "new")

        assertEquals("value", env.getDependency("new", String::class.java))
        env.removeDependency("new", String::class.java)
        assertEquals("value", env.getDependency("old", String::class.java))
        assertNull(env.getDependency("new", String::class.java))
    }

    private class Node(
        val name: String,
    ) : AbsDependencyIterator<Node>()
}
