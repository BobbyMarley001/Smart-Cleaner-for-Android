package com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model

data class DeleteResult(
    val deletedCount: Int,
    val failedPaths: List<String>
)
