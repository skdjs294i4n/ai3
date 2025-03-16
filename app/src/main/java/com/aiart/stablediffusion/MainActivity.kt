package com.aiart.stablediffusion

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aiart.stablediffusion.utils.PermissionUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Setup navigation with bottom navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        bottomNavigationView.setupWithNavController(navController)
        
        // Check storage permissions for model loading
        checkStoragePermissions()
    }
    
    /**
     * Check and request storage permissions if needed
     */
    private fun checkStoragePermissions() {
        if (!PermissionUtils.hasStoragePermissions(this)) {
            showStoragePermissionDialog()
        }
    }
    
    /**
     * Show dialog explaining why we need storage permissions
     */
    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                PermissionUtils.requestStoragePermissions(this)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                Toast.makeText(
                    this,
                    R.string.storage_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Handle permission results from requestPermissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        PermissionUtils.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Toast.makeText(
                    this,
                    R.string.storage_permission_granted,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDenied = {
                Toast.makeText(
                    this,
                    R.string.storage_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
    
    /**
     * Handle activity results for special permissions on Android 11+
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        PermissionUtils.handleActivityResult(
            this,
            requestCode,
            resultCode,
            onGranted = {
                Toast.makeText(
                    this,
                    R.string.storage_permission_granted,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDenied = {
                Toast.makeText(
                    this,
                    R.string.storage_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}
