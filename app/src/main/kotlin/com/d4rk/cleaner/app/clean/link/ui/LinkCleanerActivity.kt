package com.d4rk.cleaner.app.clean.link.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.link.domain.UrlCleaner

class LinkCleanerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        text?.let {
            val cleaned = UrlCleaner.clean(text)
            Toast.makeText(this, R.string.link_cleaned, Toast.LENGTH_SHORT).show()
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, cleaned)
            }
            val chooser = Intent.createChooser(share, getString(R.string.share_via))
            if (share.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(this, R.string.no_application_found, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
        }
        finishAndRemoveTask()
    }
}