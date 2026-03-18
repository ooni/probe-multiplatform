@file:Suppress("ktlint:standard:function-naming")

package org.ooni.engine

import kotlinx.coroutines.test.runTest
import org.ooni.testing.createTestSecureStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract test for [SecureStorage].
 *
 * Uses camelCase test names instead of backtick names with spaces because
 * D8 does not support spaces in class names prior to DEX version 040 (API 30),
 * and the app's minSdk is 24.
 */
class SecureStorageTest {
    private lateinit var storage: SecureStorage

    @BeforeTest
    fun setUp() {
        storage = createTestSecureStorage()
    }

    @AfterTest
    fun tearDown() =
        runTest {
            storage.deleteAll()
        }

    @Test
    fun firstWriteReturnsCreatedWithCorrectKey() =
        runTest {
            val result = storage.write("k", "v")
            assertTrue(result is WriteResult.Created)
            assertEquals("k", result.key)
        }

    @Test
    fun writeAndReadBackAValue() =
        runTest {
            val w = storage.write("key", "value")
            assertTrue(w !is WriteResult.Error)
            assertEquals("value", storage.read("key"))
        }

    @Test
    fun readReturnsNullForAbsentKey() =
        runTest {
            assertNull(storage.read("missing"))
        }

    @Test
    fun writeOverwritesExistingValueAndReturnsUpdatedWithCorrectKey() =
        runTest {
            storage.write("key", "first")
            val w2 = storage.write("key", "second")
            assertTrue(w2 is WriteResult.Updated)
            assertEquals("key", w2.key)
            assertEquals("second", storage.read("key"))
        }

    @Test
    fun overwritingAKeyDoesNotDuplicateItInList() =
        runTest {
            storage.write("dup", "v1")
            storage.write("dup", "v2")
            storage.write("dup", "v3")

            val keys = storage.list()
            assertEquals(1, keys.size)
            assertTrue("dup" in keys)
        }

    @Test
    fun writeAndReadValueWithSpecialCharacters() =
        runTest {
            val specialValue = "hello\nworld\t!@#\$%^&*()_+{}[]|\\:;\"'<>,.?/~`"
            storage.write("special", specialValue)
            assertEquals(specialValue, storage.read("special"))
        }

    @Test
    fun writeAndReadEmptyStringValue() =
        runTest {
            val w = storage.write("empty", "")
            assertTrue(w is WriteResult.Created)
            assertEquals("", storage.read("empty"))
        }

    @Test
    fun writeAndReadUnicodeValue() =
        runTest {
            val unicode = "مرحبا 你好 こんにちは"
            storage.write("unicode", unicode)
            assertEquals(unicode, storage.read("unicode"))
        }

    @Test
    fun multipleKeysAreStoredIndependently() =
        runTest {
            storage.write("a", "alpha")
            storage.write("b", "beta")
            storage.write("c", "gamma")

            assertEquals("alpha", storage.read("a"))
            assertEquals("beta", storage.read("b"))
            assertEquals("gamma", storage.read("c"))
        }

    @Test
    fun largeValueIsStoredAndRetrievedCorrectly() =
        runTest {
            val largeValue = "x".repeat(10_000)
            storage.write("big", largeValue)
            assertEquals(largeValue, storage.read("big"))
        }

    @Test
    fun keyWithSpacesIsHandled() =
        runTest {
            storage.write("key with spaces", "spaced value")
            assertEquals("spaced value", storage.read("key with spaces"))
        }

    @Test
    fun keyContainingNewlineIsHandled() =
        runTest {
            // newline is used as the index separator on Mac/Linux — must not corrupt the index
            storage.write("line1\nline2", "value")
            assertEquals("value", storage.read("line1\nline2"))
        }

    @Test
    fun existsReturnsTrueAfterWrite() =
        runTest {
            storage.write("present", "yes")
            assertTrue(storage.exists("present"))
        }

    @Test
    fun existsReturnsFalseForAbsentKey() =
        runTest {
            assertFalse(storage.exists("absent"))
        }

    @Test
    fun existsReflectsLatestValueAfterOverwrite() =
        runTest {
            storage.write("k", "v1")
            storage.write("k", "v2")
            assertTrue(storage.exists("k"))
        }

    @Test
    fun existsReturnsFalseAfterDelete() =
        runTest {
            storage.write("gone", "value")
            val dr = storage.delete("gone")
            assertTrue(dr is DeleteResult.Deleted)
            assertFalse(storage.exists("gone"))
        }

    @Test
    fun existsReturnsFalseAfterDeleteAll() =
        runTest {
            storage.write("a", "1")
            storage.write("b", "2")
            storage.deleteAll()
            assertFalse(storage.exists("a"))
            assertFalse(storage.exists("b"))
        }

    @Test
    fun deleteRemovesExistingKeyAndReturnsDeletedWithCorrectKey() =
        runTest {
            storage.write("toDelete", "value")
            val dr = storage.delete("toDelete")
            assertTrue(dr is DeleteResult.Deleted)
            assertEquals("toDelete", dr.key)
            assertNull(storage.read("toDelete"))
        }

    @Test
    fun deleteOnAbsentKeyReturnsNotFoundWithCorrectKey() =
        runTest {
            val dr = storage.delete("neverExisted")
            assertTrue(dr is DeleteResult.NotFound)
            assertEquals("neverExisted", dr.key)
        }

    @Test
    fun deleteTwiceOnSameKeyReturnsNotFoundOnSecondCall() =
        runTest {
            storage.write("once", "v")
            val first = storage.delete("once")
            val second = storage.delete("once")
            assertTrue(first is DeleteResult.Deleted)
            assertTrue(second is DeleteResult.NotFound)
        }

    @Test
    fun deleteOnlyRemovesTheTargetedKey() =
        runTest {
            storage.write("keep", "safe")
            storage.write("remove", "gone")
            storage.delete("remove")

            assertEquals("safe", storage.read("keep"))
            assertNull(storage.read("remove"))
        }

    @Test
    fun listReturnsEmptyWhenStorageIsEmpty() =
        runTest {
            assertEquals(emptyList(), storage.list())
        }

    @Test
    fun listReturnsAllStoredKeys() =
        runTest {
            storage.write("x", "1")
            storage.write("y", "2")
            storage.write("z", "3")

            val keys = storage.list()
            assertEquals(3, keys.size)
            assertTrue("x" in keys)
            assertTrue("y" in keys)
            assertTrue("z" in keys)
        }

    @Test
    fun listDoesNotIncludeDeletedKeys() =
        runTest {
            storage.write("keep", "value")
            storage.write("remove", "value")
            storage.delete("remove")

            val keys = storage.list()
            assertTrue("keep" in keys)
            assertFalse("remove" in keys)
        }

    @Test
    fun listIsEmptyAfterDeleteAll() =
        runTest {
            storage.write("a", "1")
            storage.write("b", "2")
            storage.deleteAll()
            assertEquals(emptyList(), storage.list())
        }

    @Test
    fun listContainsKeyWrittenAfterDeleteAll() =
        runTest {
            storage.write("old", "v")
            storage.deleteAll()
            storage.write("new", "v")

            val keys = storage.list()
            assertEquals(1, keys.size)
            assertTrue("new" in keys)
            assertFalse("old" in keys)
        }

    @Test
    fun deleteAllRemovesAllEntries() =
        runTest {
            storage.write("a", "1")
            storage.write("b", "2")
            storage.write("c", "3")

            val res = storage.deleteAll()
            assertTrue(res is DeleteAllResult.DeletedCount)
            assertEquals(3, res.count)
            assertNull(storage.read("a"))
            assertNull(storage.read("b"))
            assertNull(storage.read("c"))
        }

    @Test
    fun deleteAllOnEmptyStorageReturnsZeroCount() =
        runTest {
            val res = storage.deleteAll()
            assertTrue(res is DeleteAllResult.DeletedCount)
            assertEquals(0, res.count)
        }

    @Test
    fun deleteAllCountEqualsNumberOfDistinctKeysWritten() =
        runTest {
            storage.write("k1", "v1")
            storage.write("k2", "v2")
            storage.write("k1", "v1b") // overwrite — still one key

            val res = storage.deleteAll()
            assertTrue(res is DeleteAllResult.DeletedCount)
            assertEquals(2, res.count)
        }

    @Test
    fun deleteAllIsIdempotentSecondCallReturnsZero() =
        runTest {
            storage.write("x", "v")
            storage.deleteAll()

            val second = storage.deleteAll()
            assertTrue(second is DeleteAllResult.DeletedCount)
            assertEquals(0, second.count)
        }

    @Test
    fun writeAfterDeleteAllSucceeds() =
        runTest {
            storage.write("before", "value")
            storage.deleteAll()
            storage.write("after", "new")

            assertNull(storage.read("before"))
            assertEquals("new", storage.read("after"))
        }

    @Test
    fun readReturnsNullForEveryKeyAfterDeleteAll() =
        runTest {
            val keys = listOf("a", "b", "c")
            keys.forEach { storage.write(it, "v") }
            storage.deleteAll()
            keys.forEach { assertNull(storage.read(it)) }
        }
}
