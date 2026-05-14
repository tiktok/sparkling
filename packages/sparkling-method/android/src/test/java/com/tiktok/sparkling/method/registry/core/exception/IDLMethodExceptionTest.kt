// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.exception

import org.junit.Assert.*
import org.junit.Test

class IDLMethodExceptionTest {
    @Test
    fun testIDLMethodExceptionWithMessage() {
        // Given
        val errorMessage = "Test IDL method error"

        // When
        val exception = IDLMethodException(errorMessage ?: "")

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertTrue("Should be instance of Exception", exception is Exception)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
    }

    @Test
    fun testIDLMethodExceptionWithMessageAndCause() {
        // Given
        val errorMessage = "Test IDL method error with cause"
        val cause = RuntimeException("Root cause")

        // When
        val exception = IDLMethodException(errorMessage ?: "").apply { initCause(cause) }

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertEquals("Cause should match", cause, exception.cause)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
    }

    @Test
    fun testIDLMethodExceptionWithNullMessage() {
        // Given
        val errorMessage: String? = null

        // When
        val exception = IDLMethodException(errorMessage ?: "")

        // Then
        assertEquals("Error message should be empty string", "", exception.message)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
    }

    @Test
    fun testIDLMethodExceptionWithEmptyMessage() {
        // Given
        val errorMessage = ""

        // When
        val exception = IDLMethodException(errorMessage ?: "")

        // Then
        assertEquals("Error message should be empty", "", exception.message)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
    }

    @Test
    fun testIDLMethodExceptionInheritance() {
        // Given
        val exception = IDLMethodException("Test")

        // When & Then
        assertTrue("Should be instance of Exception", exception is Exception)
        assertTrue("Should be instance of Throwable", exception is Throwable)
        assertEquals("Class name should match", "IDLMethodException", exception.javaClass.simpleName)
    }

    @Test
    fun testIDLMethodExceptionStackTrace() {
        // Given
        val exception = IDLMethodException("Test stack trace")

        // When
        val stackTrace = exception.stackTrace

        // Then
        assertNotNull("Stack trace should not be null", stackTrace)
        assertTrue("Stack trace should not be empty", stackTrace.isNotEmpty())
        assertTrue(
            "First stack trace element should contain test method",
            stackTrace[0].methodName.contains("testIDLMethodExceptionStackTrace"),
        )
    }

    @Test
    fun testIDLMethodExceptionToString() {
        // Given
        val errorMessage = "Test toString method"
        val exception = IDLMethodException(errorMessage ?: "")

        // When
        val stringRepresentation = exception.toString()

        // Then
        assertNotNull("String representation should not be null", stringRepresentation)
        assertTrue(
            "String should contain class name",
            stringRepresentation.contains("IDLMethodException"),
        )
        assertTrue(
            "String should contain error message",
            stringRepresentation.contains(errorMessage),
        )
    }

    @Test
    fun testThrowAndCatchIDLMethodException() {
        // Given
        val errorMessage = "Test throw and catch"
        var caughtException: IDLMethodException? = null

        // When
        try {
            throw IDLMethodException(errorMessage)
        } catch (e: IDLMethodException) {
            caughtException = e
        }

        // Then
        assertNotNull("Exception should be caught", caughtException)
        assertEquals("Error message should match", errorMessage, caughtException?.message)
        assertTrue("Should be IDLMethodException type", caughtException is IDLMethodException)
    }
}

class IllegalInputParamExceptionTest {
    @Test
    fun testIllegalInputParamExceptionWithMessage() {
        // Given
        val errorMessage = "Invalid input parameter"

        // When
        val exception = IllegalInputParamException(errorMessage)

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of IllegalInputParamException", exception is IllegalInputParamException)
    }

    @Test
    fun testIllegalInputParamExceptionWithMessageAndCause() {
        // Given
        val errorMessage = "Invalid input parameter with cause"
        val cause = IllegalArgumentException("Cause of invalid param")

        // When
        val exception = IllegalInputParamException(errorMessage).apply { initCause(cause) }

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertEquals("Cause should match", cause, exception.cause)
        assertTrue("Should be instance of IllegalInputParamException", exception is IllegalInputParamException)
    }

    @Test
    fun testIllegalInputParamExceptionInheritance() {
        // Given
        val exception = IllegalInputParamException("Test")

        // When & Then
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of Exception", exception is Exception)
        assertEquals("Class name should match", "IllegalInputParamException", exception.javaClass.simpleName)
    }

    @Test
    fun testThrowAndCatchIllegalInputParamException() {
        // Given
        val errorMessage = "Test input param exception"
        var caughtException: IllegalInputParamException? = null

        // When
        try {
            throw IllegalInputParamException(errorMessage)
        } catch (e: IllegalInputParamException) {
            caughtException = e
        }

        // Then
        assertNotNull("Exception should be caught", caughtException)
        assertEquals("Error message should match", errorMessage, caughtException?.message)
    }
}

class IllegalOperationExceptionTest {
    @Test
    fun testIllegalOperationExceptionWithMessage() {
        // Given
        val errorMessage = "Illegal operation performed"

        // When
        val exception = IllegalOperationException(errorMessage)

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of IllegalOperationException", exception is IllegalOperationException)
    }

    @Test
    fun testIllegalOperationExceptionWithMessageAndCause() {
        // Given
        val errorMessage = "Illegal operation with cause"
        val cause = UnsupportedOperationException("Operation not supported")

        // When
        val exception = IllegalOperationException(errorMessage).apply { initCause(cause) }

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertEquals("Cause should match", cause, exception.cause)
        assertTrue("Should be instance of IllegalOperationException", exception is IllegalOperationException)
    }

    @Test
    fun testIllegalOperationExceptionInheritance() {
        // Given
        val exception = IllegalOperationException("Test")

        // When & Then
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of Exception", exception is Exception)
        assertEquals("Class name should match", "IllegalOperationException", exception.javaClass.simpleName)
    }

    @Test
    fun testThrowAndCatchIllegalOperationException() {
        // Given
        val errorMessage = "Test operation exception"
        var caughtException: IllegalOperationException? = null

        // When
        try {
            throw IllegalOperationException(errorMessage)
        } catch (e: IllegalOperationException) {
            caughtException = e
        }

        // Then
        assertNotNull("Exception should be caught", caughtException)
        assertEquals("Error message should match", errorMessage, caughtException?.message)
    }
}

class IllegalOutputParamExceptionTest {
    @Test
    fun testIllegalOutputParamExceptionWithMessage() {
        // Given
        val errorMessage = "Invalid output parameter"

        // When
        val exception = IllegalOutputParamException(errorMessage)

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of IllegalOutputParamException", exception is IllegalOutputParamException)
    }

    @Test
    fun testIllegalOutputParamExceptionWithMessageAndCause() {
        // Given
        val errorMessage = "Invalid output parameter with cause"
        val cause = ClassCastException("Cannot cast output param")

        // When
        val exception = IllegalOutputParamException(errorMessage).apply { initCause(cause) }

        // Then
        assertEquals("Error message should match", errorMessage, exception.message)
        assertEquals("Cause should match", cause, exception.cause)
        assertTrue("Should be instance of IllegalOutputParamException", exception is IllegalOutputParamException)
    }

    @Test
    fun testIllegalOutputParamExceptionInheritance() {
        // Given
        val exception = IllegalOutputParamException("Test")

        // When & Then
        assertTrue("Should be instance of IDLMethodException", exception is IDLMethodException)
        assertTrue("Should be instance of Exception", exception is Exception)
        assertEquals("Class name should match", "IllegalOutputParamException", exception.javaClass.simpleName)
    }

    @Test
    fun testThrowAndCatchIllegalOutputParamException() {
        // Given
        val errorMessage = "Test output param exception"
        var caughtException: IllegalOutputParamException? = null

        // When
        try {
            throw IllegalOutputParamException(errorMessage)
        } catch (e: IllegalOutputParamException) {
            caughtException = e
        }

        // Then
        assertNotNull("Exception should be caught", caughtException)
        assertEquals("Error message should match", errorMessage, caughtException?.message)
    }
}
