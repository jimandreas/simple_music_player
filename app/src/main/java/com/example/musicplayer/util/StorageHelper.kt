package com.example.musicplayer.util

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.net.Uri

data class StorageOption(
    val name: String,
    val uuid: String?,
    val isPrimary: Boolean,
    val initialUri: Uri?
)

object StorageHelper {

    private fun getAttributedContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("attributionTag")
        } else {
            context
        }
    }

    fun getStorageOptions(context: Context): List<StorageOption> {
        val attributedContext = getAttributedContext(context)
        val storageManager = attributedContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes

        return volumes.filter { it.state == "mounted" }.map { volume ->
            StorageOption(
                name = volume.getDescription(attributedContext) ?: if (volume.isPrimary) "Internal Storage" else "SD Card",
                uuid = volume.uuid,
                isPrimary = volume.isPrimary,
                initialUri = buildInitialUri(volume)
            )
        }
    }

    private fun buildInitialUri(volume: StorageVolume): Uri? {
        // For primary storage, use the primary external storage URI
        // For SD cards, use the volume's UUID to build a URI
        return if (volume.isPrimary) {
            DocumentsContract.buildRootUri(
                "com.android.externalstorage.documents",
                "primary"
            )
        } else {
            volume.uuid?.let { uuid ->
                DocumentsContract.buildRootUri(
                    "com.android.externalstorage.documents",
                    uuid
                )
            }
        }
    }

    fun hasMultipleStorageOptions(context: Context): Boolean {
        return getStorageOptions(context).size > 1
    }
}
