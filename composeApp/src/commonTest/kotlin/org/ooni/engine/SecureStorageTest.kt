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
    fun `write and read back a value`() =
        runTest {
            assertTrue(storage.write("key", "value"))
            assertEquals("value", storage.read("key"))
        }

    @Test
    fun `read returns null for absent key`() =
        runTest {
            assertNull(storage.read("missing"))
        }

    @Test
    fun `write overwrites existing value`() =
        runTest {
            storage.write("key", "first")
            storage.write("key", "second")
            assertEquals("second", storage.read("key"))
        }

    @Test
    fun `write and read value with special characters`() =
        runTest {
            val specialValue = "hello\nworld\t!@#\$%^&*()_+{}[]|\\:;\"'<>,.?/~`"
            storage.write("special", specialValue)
            assertEquals(specialValue, storage.read("special"))
        }

    @Test
    fun `write and read empty string value`() =
        runTest {
            storage.write("empty", "")
            assertEquals("", storage.read("empty"))
        }

    @Test
    fun `write and read unicode value`() =
        runTest {
            val unicode = "مرحبا 你好 こんにちは"
            storage.write("unicode", unicode)
            assertEquals(unicode, storage.read("unicode"))
        }

    @Test
    fun `multiple keys are stored independently`() =
        runTest {
            storage.write("a", "alpha")
            storage.write("b", "beta")
            storage.write("c", "gamma")

            assertEquals("alpha", storage.read("a"))
            assertEquals("beta", storage.read("b"))
            assertEquals("gamma", storage.read("c"))
        }

    @Test
    fun `exists returns true after write`() =
        runTest {
            storage.write("present", "yes")
            assertTrue(storage.exists("present"))
        }

    @Test
    fun `exists returns false for absent key`() =
        runTest {
            assertFalse(storage.exists("absent"))
        }

    @Test
    fun `exists returns false after delete`() =
        runTest {
            storage.write("gone", "value")
            storage.delete("gone")
            assertFalse(storage.exists("gone"))
        }

    @Test
    fun `delete removes an existing key`() =
        runTest {
            storage.write("toDelete", "value")
            assertTrue(storage.delete("toDelete"))
            assertNull(storage.read("toDelete"))
        }

    @Test
    fun `delete on absent key returns true`() =
        runTest {
            // Deleting a non-existent key is not an error
            assertTrue(storage.delete("neverExisted"))
        }

    @Test
    fun `delete only removes the targeted key`() =
        runTest {
            storage.write("keep", "safe")
            storage.write("remove", "gone")
            storage.delete("remove")

            assertEquals("safe", storage.read("keep"))
            assertNull(storage.read("remove"))
        }

    @Test
    fun `list returns empty when storage is empty`() =
        runTest {
            assertEquals(emptyList(), storage.list())
        }

    @Test
    fun `list returns all stored keys`() =
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
    fun `list does not include deleted keys`() =
        runTest {
            storage.write("keep", "value")
            storage.write("remove", "value")
            storage.delete("remove")

            val keys = storage.list()
            assertTrue("keep" in keys)
            assertFalse("remove" in keys)
        }

    @Test
    fun `deleteAll removes all entries`() =
        runTest {
            storage.write("a", "1")
            storage.write("b", "2")
            storage.write("c", "3")

            assertTrue(storage.deleteAll())
            assertEquals(emptyList(), storage.list())
            assertNull(storage.read("a"))
            assertNull(storage.read("b"))
            assertNull(storage.read("c"))
        }

    @Test
    fun `deleteAll on empty storage returns true`() =
        runTest {
            assertTrue(storage.deleteAll())
        }

    @Test
    fun `write after deleteAll succeeds`() =
        runTest {
            storage.write("before", "value")
            storage.deleteAll()
            storage.write("after", "new")

            assertNull(storage.read("before"))
            assertEquals("new", storage.read("after"))
        }

    @Test
    fun `large value is stored and retrieved correctly`() =
        runTest {
            val largeValue = "x".repeat(10_000)
            storage.write("big", largeValue)
            assertEquals(largeValue, storage.read("big"))
        }

    @Test
    fun `key with spaces is handled`() =
        runTest {
            storage.write("key with spaces", "spaced value")
            assertEquals("spaced value", storage.read("key with spaces"))
        }
}
