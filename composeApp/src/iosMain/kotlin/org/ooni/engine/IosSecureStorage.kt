package org.ooni.engine

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureStorage(
    private val appId: String,
) : SecureStorage {
    override suspend fun read(key: String): String? =
        withContext(Dispatchers.IO) {
            val query = buildQuery(key) {
                addEntry(kSecReturnData, kCFBooleanTrue)
                addEntry(kSecMatchLimit, kSecMatchLimitOne)
            }
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                CFBridgingRelease(query)
                if (status == errSecSuccess) {
                    val data = CFBridgingRelease(result.value) as? platform.Foundation.NSData
                    data?.let {
                        val nsString = NSString.create(it, NSUTF8StringEncoding)
                        nsString?.toString()
                    }
                } else {
                    null
                }
            }
        }

    override suspend fun write(
        key: String,
        value: String,
    ): WriteResult =
        withContext(Dispatchers.IO) {
            val nsString = NSString.create(string = value)
            val valueData = nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: return@withContext WriteResult.Error(key, "encode failed")
            val valueDataRef = CFBridgingRetain(valueData)

            // Use SecItemUpdate and SecItemAdd paths but update index only on success
            val searchQuery = buildQuery(key)
            val attributes = buildCFDictionary { addEntry(kSecValueData, valueDataRef) }
            var status = SecItemUpdate(searchQuery, attributes)
            CFBridgingRelease(searchQuery)
            CFBridgingRelease(attributes)

            if (status == errSecItemNotFound) {
                val addQuery = buildQuery(key) { addEntry(kSecValueData, valueDataRef) }
                status = SecItemAdd(addQuery, null)
                CFBridgingRelease(addQuery)
                CFBridgingRelease(valueDataRef)
                return@withContext if (status == errSecSuccess) {
                    WriteResult.Created(key)
                } else {
                    WriteResult.Error(key, "osstatus=$status")
                }
            }

            CFBridgingRelease(valueDataRef)
            return@withContext if (status == errSecSuccess) {
                WriteResult.Updated(key)
            } else {
                WriteResult.Error(key, "osstatus=$status")
            }
        }

    override suspend fun exists(key: String): Boolean =
        withContext(Dispatchers.IO) {
            val query = buildQuery(key) {
                addEntry(kSecMatchLimit, kSecMatchLimitOne)
            }
            val status = SecItemCopyMatching(query, null)
            CFBridgingRelease(query)
            status == errSecSuccess
        }

    override suspend fun delete(key: String): DeleteResult =
        withContext(Dispatchers.IO) {
            val query = buildQuery(key)
            val status = SecItemDelete(query)
            CFBridgingRelease(query)
            when (status) {
                errSecSuccess -> DeleteResult.Deleted(key)
                errSecItemNotFound -> DeleteResult.NotFound(key)
                else -> DeleteResult.Error(key, "osstatus=$status")
            }
        }

    override suspend fun list(): List<String> =
        withContext(Dispatchers.IO) {
            val query = buildServiceQuery {
                addEntry(kSecReturnAttributes, kCFBooleanTrue)
                addEntry(kSecMatchLimit, kSecMatchLimitAll)
            }
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                CFBridgingRelease(query)
                if (status == errSecSuccess) {
                    @Suppress("UNCHECKED_CAST")
                    val items = CFBridgingRelease(result.value) as? List<Map<Any?, *>>
                    items?.mapNotNull { it[kSecAttrAccount as Any] as? String } ?: emptyList()
                } else {
                    emptyList()
                }
            }
        }

    override suspend fun deleteAll(): DeleteAllResult =
        withContext(Dispatchers.IO) {
            val itemsQuery = buildServiceQuery {
                addEntry(kSecReturnAttributes, kCFBooleanTrue)
                addEntry(kSecMatchLimit, kSecMatchLimitAll)
            }
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(itemsQuery, result.ptr)
                CFBridgingRelease(itemsQuery)
                if (status != errSecSuccess) {
                    // if nothing found treat as zero deleted
                    if (status == errSecItemNotFound) return@withContext DeleteAllResult.DeletedCount(0)
                    return@withContext DeleteAllResult.Error("enumeration failed: osstatus=$status")
                }

                @Suppress("UNCHECKED_CAST")
                val items = CFBridgingRelease(result.value) as? List<Map<Any?, *>>
                val keys = items?.mapNotNull { it[kSecAttrAccount as Any] as? String } ?: emptyList()

                // Attempt delete; if SecItemDelete returns notfound or success treat accordingly
                val serviceQuery = buildServiceQuery()
                val delStatus = SecItemDelete(serviceQuery)
                CFBridgingRelease(serviceQuery)
                if (delStatus == errSecSuccess || delStatus == errSecItemNotFound) {
                    DeleteAllResult.DeletedCount(keys.size)
                } else {
                    DeleteAllResult.Error("deleteAll failed: osstatus=$delStatus")
                }
            }
        }

    private inline fun buildQuery(
        key: String,
        block: CFMutableDictionaryScope.() -> Unit = {},
    ): CFDictionaryRef =
        buildCFDictionary {
            addEntry(kSecClass, kSecClassGenericPassword)
            addEntry(kSecAttrService, CFBridgingRetain(NSString.create(string = appId)))
            addEntry(kSecAttrAccount, CFBridgingRetain(NSString.create(string = key)))
            block()
        }

    private inline fun buildServiceQuery(block: CFMutableDictionaryScope.() -> Unit = {}): CFDictionaryRef =
        buildCFDictionary {
            addEntry(kSecClass, kSecClassGenericPassword)
            addEntry(kSecAttrService, CFBridgingRetain(NSString.create(string = appId)))
            block()
        }

    private inline fun buildCFDictionary(block: CFMutableDictionaryScope.() -> Unit): CFDictionaryRef {
        val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFMutableDictionaryScope(dict).block()
        return dict
    }

    private value class CFMutableDictionaryScope(
        val dict: platform.CoreFoundation.CFMutableDictionaryRef,
    ) {
        fun addEntry(
            key: CFStringRef?,
            value: platform.CoreFoundation.CFTypeRef?,
        ) {
            platform.CoreFoundation.CFDictionaryAddValue(dict, key, value)
        }
    }
}
