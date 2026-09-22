package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.InvalidLiteral
import printscript.reader.CharPosition
import printscript.support.TEST_LOCATION
import printscript.support.err
import printscript.support.ok
import printscript.syntax.Location

class ValueCoercionTest {
    private fun coerce(
        value: RuntimeValue,
        declaredType: String?,
        location: Location = TEST_LOCATION,
    ) = ValueCoercion.toDeclared(value, declaredType, location)

    private fun raw(
        text: String,
        declaredType: String?,
    ) = coerce(RawInputValue(text), declaredType)

    // --- valores que no son crudos ---

    @Test
    fun `non raw values pass through untouched`() {
        val values = listOf(StringValue("hola"), NumberValue(1.0), BooleanValue(true), UnitValue, UninitializedValue)

        values.forEach { value ->
            assertSame(value, ok(coerce(value, "string")))
        }
    }

    @Test
    fun `non raw value is not converted even when the declared type differs`() {
        assertEquals(StringValue("hola"), ok(coerce(StringValue("hola"), "number")))
        assertEquals(NumberValue(1.0), ok(coerce(NumberValue(1.0), "boolean")))
    }

    @Test
    fun `non raw value with unknown declared type still passes`() {
        assertEquals(NumberValue(1.0), ok(coerce(NumberValue(1.0), "float")))
    }

    // --- sin tipo declarado ---

    @Test
    fun `raw value without declared type becomes a string`() {
        assertEquals(StringValue("42"), ok(raw("42", null)))
        assertEquals(StringValue("true"), ok(raw("true", null)))
    }

    @Test
    fun `raw value without declared type keeps the text as is`() {
        assertEquals(StringValue("  espacios  "), ok(raw("  espacios  ", null)))
        assertEquals(StringValue(""), ok(raw("", null)))
    }

    // --- string ---

    @Test
    fun `string keeps the raw text without trimming`() {
        assertEquals(StringValue("  hola  "), ok(raw("  hola  ", "string")))
    }

    @Test
    fun `empty raw text is a valid string`() {
        assertEquals(StringValue(""), ok(raw("", "string")))
    }

    // --- number ---

    @Test
    fun `number parses integers and decimals`() {
        assertEquals(NumberValue(42.0), ok(raw("42", "number")))
        assertEquals(NumberValue(1.5), ok(raw("1.5", "number")))
        assertEquals(NumberValue(-3.0), ok(raw("-3", "number")))
    }

    @Test
    fun `number trims surrounding whitespace`() {
        assertEquals(NumberValue(7.0), ok(raw("  7  ", "number")))
        assertEquals(NumberValue(7.0), ok(raw("\n7\t", "number")))
    }

    @Test
    fun `number rejects text that is not a number`() {
        assertTrue(err(raw("abc", "number")) is InvalidLiteral)
        assertTrue(err(raw("12abc", "number")) is InvalidLiteral)
        assertTrue(err(raw("1 2", "number")) is InvalidLiteral)
        assertTrue(err(raw("", "number")) is InvalidLiteral)
        assertTrue(err(raw("   ", "number")) is InvalidLiteral)
    }

    @Test
    fun `number rejects non finite values`() {
        assertTrue(err(raw("Infinity", "number")) is InvalidLiteral)
        assertTrue(err(raw("-Infinity", "number")) is InvalidLiteral)
        assertTrue(err(raw("NaN", "number")) is InvalidLiteral)
    }

    // --- boolean ---

    @Test
    fun `boolean parses true and false`() {
        assertEquals(BooleanValue(true), ok(raw("true", "boolean")))
        assertEquals(BooleanValue(false), ok(raw("false", "boolean")))
    }

    @Test
    fun `boolean trims surrounding whitespace`() {
        assertEquals(BooleanValue(true), ok(raw("  true  ", "boolean")))
    }

    @Test
    fun `boolean rejects other casings`() {
        assertTrue(err(raw("True", "boolean")) is InvalidLiteral)
        assertTrue(err(raw("TRUE", "boolean")) is InvalidLiteral)
        assertTrue(err(raw("FALSE", "boolean")) is InvalidLiteral)
    }

    @Test
    fun `boolean rejects text that is not a boolean`() {
        assertTrue(err(raw("Hola", "boolean")) is InvalidLiteral)
        assertTrue(err(raw("1", "boolean")) is InvalidLiteral)
        assertTrue(err(raw("", "boolean")) is InvalidLiteral)
    }

    // --- tipo desconocido ---

    @Test
    fun `unknown declared type rejects the raw value`() {
        assertTrue(err(raw("42", "float")) is InvalidLiteral)
        assertTrue(err(raw("42", "")) is InvalidLiteral)
        assertTrue(err(raw("42", "Number")) is InvalidLiteral)
    }

    // --- detalle del error ---

    @Test
    fun `error carries the untrimmed literal and the given location`() {
        val location = Location(CharPosition(7, 3), CharPosition(7, 9))

        val error = err(coerce(RawInputValue("  Hola  "), "boolean", location)) as InvalidLiteral

        assertEquals("  Hola  ", error.literal)
        assertEquals(location, error.location)
    }
}
