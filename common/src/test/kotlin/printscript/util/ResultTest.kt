package printscript.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultTest {
    @Test
    fun `Ok isOk is true`() {
        assertTrue(Result.Ok(1).isOk)
    }

    @Test
    fun `Err isOk is false`() {
        assertFalse(Result.Err("boom").isOk)
    }

    @Test
    fun `map transforms Ok value`() {
        val mapped = Result.Ok(2).map { it * 3 }

        assertEquals(Result.Ok(6), mapped)
    }

    @Test
    fun `map changes Ok value type`() {
        val mapped = Result.Ok(2).map { it.toString() }

        assertEquals(Result.Ok("2"), mapped)
    }

    @Test
    fun `map leaves Err unchanged and does not invoke transform`() {
        val err: Result<Int, String> = Result.Err("boom")
        var called = false

        val mapped =
            err.map {
                called = true
                it * 3
            }

        assertEquals(Result.Err("boom"), mapped)
        assertFalse(called)
    }

    @Test
    fun `fold on Ok uses onOk`() {
        val folded =
            Result.Ok(2).fold(
                onOk = { it + 1 },
                onErr = { -1 },
            )

        assertEquals(3, folded)
    }

    @Test
    fun `fold on Err uses onErr`() {
        val folded =
            Result.Err("boom").fold(
                onOk = { 0 },
                onErr = { it.length },
            )

        assertEquals(4, folded)
    }
}

class ReportTest {
    @Test
    fun `isOk is true when there are no errors`() {
        val report = Report(value = "ok", errors = emptyList<String>())

        assertTrue(report.isOk)
    }

    @Test
    fun `isOk is true for an empty Report`() {
        assertTrue(Report<String, String>().isOk)
    }

    @Test
    fun `isOk is false when there are errors`() {
        val report = Report(value = "ok", errors = listOf("e1", "e2"))

        assertFalse(report.isOk)
    }
}
