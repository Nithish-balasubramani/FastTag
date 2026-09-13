package com.example

import com.example.data.repository.SerialRangeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialRangeHelperTest {

    @Test
    fun `test numeric range expansion`() {
        val result = SerialRangeHelper.expandRange("100001", "100005")
        assertTrue(result.isValid)
        assertEquals(5, result.quantity)
        val list = result.serials
        assertEquals(5, list.size)
        assertEquals("100001", list[0])
        assertEquals("100002", list[1])
        assertEquals("100003", list[2])
        assertEquals("100004", list[3])
        assertEquals("100005", list[4])
    }

    @Test
    fun `test prefixed range expansion`() {
        val result = SerialRangeHelper.expandRange("TAG001", "TAG005")
        assertTrue(result.isValid)
        assertEquals(5, result.quantity)
        val list = result.serials
        assertEquals(5, list.size)
        assertEquals("TAG001", list[0])
        assertEquals("TAG005", list[4])
    }

    @Test
    fun `test invalid inverted range fails`() {
        val result = SerialRangeHelper.expandRange("100050", "100001")
        assertFalse(result.isValid)
    }

    @Test
    fun `test prefix mismatch fails`() {
        val result = SerialRangeHelper.expandRange("AAA100", "BBB105")
        assertFalse(result.isValid)
    }
}
