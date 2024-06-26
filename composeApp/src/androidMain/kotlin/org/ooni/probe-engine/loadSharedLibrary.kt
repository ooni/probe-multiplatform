package org.internetok.iokapp

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

fun extractLibrary(context: Context, libName: String, destination: File): Boolean {
    val apks = HashSet<String>()
    if (context.applicationInfo.sourceDir != null) {
        apks.add(context.applicationInfo.sourceDir)
    }
    if (context.applicationInfo.splitSourceDirs != null) {
        apks.addAll(context.applicationInfo.splitSourceDirs!!.toList())
    }

    for (abi in Build.SUPPORTED_ABIS) {
        for (apk in apks) {
            val zipFile = ZipFile(File(apk), ZipFile.OPEN_READ)
            val mappedLibName = System.mapLibraryName(libName)
            val libZipPath = "lib" + File.separatorChar + abi + File.separatorChar + mappedLibName
            val zipEntry = zipFile.getEntry(libZipPath) ?: continue
            Log.d(TAG, "Extracting apk:/$libZipPath to ${destination.absolutePath}")
            try {
                val outFile = FileOutputStream(destination)
                val inFile = zipFile.getInputStream(zipEntry)
                val buffer = ByteArray(1024 * 32)
                while (true) {
                    val len = inFile.read(buffer)
                    if (len == -1) {
                        break
                    }
                    outFile.write(buffer, 0, len)
                }
                outFile.fd.sync()
            } catch (e : Exception) {
                Log.e(TAG, "failed to extract zip ${e.message}")
            }
            return true
        }
    }
    return false
}

fun extractThenLoadFromApk(context: Context, libName: String) {
    var f: File? = null
    try {
        f = File.createTempFile("lib", ".so", context.getCodeCacheDir())
        if (extractLibrary(context, libName, f)) {
            val libPath = f.getAbsolutePath()
            Log.d(TAG, "loading lib from")
            System.load(libPath)
            Log.d(TAG, "library was loaded at ${libPath}")
            return
        }
    } catch (e: Exception) {
        Log.d(TAG, "Failed to load library apk:/$libName", e)
        throw RuntimeException(e)
    } finally {
        f?.delete()
    }
}

fun loadSharedLibrary(context: Context, libName: String) {
    try {
        System.loadLibrary(libName)
        Log.d(TAG, "library was loaded by name ${libName}")
        return
    } catch (e: UnsatisfiedLinkError) {
        Log.d(TAG, "Failed to load library normally, so attempting to extract from apk", e)
        try {
            extractThenLoadFromApk(context, libName)
            return
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
