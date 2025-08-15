package com.d4rk.cleaner.app.clean.whatsapp.summary.domain.repository

import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DeleteResult
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.WhatsAppMediaSummary
import com.d4rk.cleaner.core.domain.repository.FileDeletionRepository
import java.io.File

interface WhatsAppCleanerRepository : FileDeletionRepository<DeleteResult> {
    suspend fun getMediaSummary(): WhatsAppMediaSummary
    suspend fun listMediaFiles(type: String, offset: Int, limit: Int): List<File>
}
