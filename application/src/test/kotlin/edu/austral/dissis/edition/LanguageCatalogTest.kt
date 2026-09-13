package edu.austral.dissis.edition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.edition.LanguageCatalog
import printscript.edition.LanguageVersion
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.io.DefaultSideEffectManager
import printscript.util.Result
import printscript.util.map

class LanguageCatalogTest {
    private val sideEffects = DefaultSideEffectManager()
    private val v10 = LanguageCatalog.v10(sideEffects)

    fun getInterpreter(version: String): Result<Interpreter, RuntimeError> =
        LanguageCatalog.of(version, sideEffects).map { kit ->
            DefaultInterpreterFactory.create(kit.evaluators, kit.executors)
        }

    @Test
    fun `parse 1 dot 0 into major 1 minor 0`() {
        val parsed = LanguageVersion.parse("1.0")
        assertTrue(parsed is Result.Ok)
        assertEquals(LanguageVersion(1, 0), (parsed as Result.Ok).value)
    }

    @Test
    fun `parse bare 1 is LanguageVersionNotFound`() {
        val parsed = LanguageVersion.parse("1")
        assertTrue(parsed is Result.Err)
        assertTrue((parsed as Result.Err).error is LanguageVersionNotFound)
    }

    @Test
    fun `parse 1 dot 1 into major 1 minor 1`() {
        val parsed = LanguageVersion.parse("1.1")
        assertTrue(parsed is Result.Ok)
        assertEquals(LanguageVersion(1, 1), (parsed as Result.Ok).value)
    }

    @Test
    fun `parse garbage is LanguageVersionNotFound`() {
        val parsed = LanguageVersion.parse("nope")
        assertTrue(parsed is Result.Err)
        assertTrue((parsed as Result.Err).error is LanguageVersionNotFound)
    }

    @Test
    fun `of 1 dot 0 returns the v10 kit`() {
        val kit = LanguageCatalog.of("1.0", sideEffects)
        assertTrue(kit is Result.Ok)
        val value = (kit as Result.Ok).value
        assertEquals(LanguageVersion(1, 0), value.version)
        assertEquals("1.0", value.resourceSuffix)
        assertEquals(v10.evaluators.size, value.evaluators.size)
        assertEquals(v10.executors.size, value.executors.size)
    }

    @Test
    fun `of bare 1 is LanguageVersionNotFound`() {
        val kit = LanguageCatalog.of("1", sideEffects)
        assertTrue(kit is Result.Err)
        assertTrue((kit as Result.Err).error is LanguageVersionNotFound)
    }

    @Test
    fun `of 1 dot 1 is a copy of v10 with version 1 dot 1`() {
        val kit = (LanguageCatalog.of("1.1", sideEffects) as Result.Ok).value
        assertEquals(LanguageVersion(1, 1), kit.version)
        assertEquals("1.1", kit.resourceSuffix)
        assertEquals(v10.parserHandlers.size, kit.parserHandlers.size)
        assertEquals(v10.evaluators.size, kit.evaluators.size)
        assertEquals(v10.executors.size, kit.executors.size)
    }

    @Test
    fun `of 2 dot 0 is LanguageVersionNotFound`() {
        val kit = LanguageCatalog.of("2.0", sideEffects)
        assertTrue(kit is Result.Err)
        assertTrue((kit as Result.Err).error is LanguageVersionNotFound)
    }

    @Test
    fun `of 1 dot 9 is LanguageVersionNotFound`() {
        val kit = LanguageCatalog.of("1.9", sideEffects)
        assertTrue(kit is Result.Err)
        assertTrue((kit as Result.Err).error is LanguageVersionNotFound)
    }

    @Test
    fun `InterpreterFactory create 1 dot 0 is Ok`() {
        val result = getInterpreter("1.0")
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `InterpreterFactory create 1 dot 1 is Ok`() {
        val result = getInterpreter("1.1")
        assertTrue(result is Result.Ok)
    }
}
