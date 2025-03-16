package com.aiart.stablediffusion.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Utility class for handling permissions
 */
object PermissionUtils {

    const val STORAGE_PERMISSION_CODE = 100
    const val MANAGE_STORAGE_REQUEST_CODE = 101

    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses MANAGE_EXTERNAL_STORAGE permission
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 uses READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 and below use READ and WRITE permissions
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request storage permissions appropriate for the Android version
     */
    fun requestStoragePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - MANAGE_EXTERNAL_STORAGE permission
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE)
            } catch (e: Exception) {
                // If direct package access fails, open general storage settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE)
            }
        } else {
            // Android 10 and below - use traditional runtime permissions
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            
            ActivityCompat.requestPermissions(activity, permissions, STORAGE_PERMISSION_CODE)
        }
    }
    
    /**
     * Fragment version of storage permission request
     */
    fun requestStoragePermission(fragment: Fragment) {
        fragment.activity?.let { activity ->
            requestStoragePermissions(activity)
        }
    }
    
    /**
     * Check if storage permission is granted - for fragments
     */
    fun checkStoragePermission(fragment: Fragment): Boolean {
        return fragment.context?.let { hasStoragePermissions(it) } ?: false
    }

    /**
     * Handle permission result from requestPermissions
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }

    /**
     * Handle activity result from the MANAGE_EXTERNAL_STORAGE permission request
     */
    fun handleActivityResult(
        context: Context,
        requestCode: Int,
        resultCode: Int,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
}