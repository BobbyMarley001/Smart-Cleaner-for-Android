package com.d4rk.cleaner.app.clean.scanner.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.DeleteFilesUseCase
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import com.google.android.material.color.MaterialColors
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Worker responsible for deleting files or moving them to the trash.
 *
 * A notification with a determinate progress bar is shown for the entire
 * duration of the work and is updated as files are processed. Once the
 * operation finishes, the notification is updated with the final result and
 * dismissed after a short delay.
 */
class FileCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val cleaningManager: CleaningManager by inject()

    override suspend fun doWork(): Result {
        val rawPaths = inputData.getStringArray(KEY_PATHS)
        if (rawPaths.isNullOrEmpty()) {
            Log.e(TAG, "NO_DATA: input=$inputData")
            return Result.failure(
                Data.Builder().putString(KEY_ERROR, Errors.UseCase.NO_DATA.toString()).build(),
            )
        }
        val action = inputData.getString(KEY_ACTION) ?: ACTION_DELETE
        val paths = rawPaths.toList()
        println("FileCleanupWorker ---> Received paths: $paths")
        val files = mutableListOf<File>()
        var hasNonProtectedPath = false
        for (path in paths) {
            println("FileCleanupWorker ---> Checking path: $path")
            val file = File(path)
            if (file.isProtectedAndroidDir()) {
                Log.i(TAG, "Skipping protected path: ${file.absolutePath}")
                continue
            }
            hasNonProtectedPath = true
            val exists = file.exists()
            val isFile = file.isFile
            val isDirectory = file.isDirectory
            println("FileCleanupWorker ---> File exists: $exists isFile: $isFile isDirectory: $isDirectory")
            if (exists) {
                println("FileCleanupWorker ---> canRead: ${file.canRead()} canWrite: ${file.canWrite()}")
                if (isDirectory) {
                    println("FileCleanupWorker ---> directory children: ${file.listFiles()?.size ?: 0}")
                }
                files += file
            }
        }
        if (files.isEmpty()) {
            return if (hasNonProtectedPath) {
                Log.e(TAG, "NO_DATA: no valid files for paths=$paths")
                Result.failure(
                    Data.Builder().putString(KEY_ERROR, Errors.UseCase.NO_DATA.toString()).build(),
                )
            } else {
                CleaningEventBus.notifyCleaned(success = true)
                Result.success()
            }
        }

        createChannelIfNeeded()
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_cleaner_notify)
            .setContentTitle(applicationContext.getString(R.string.cleaning))
            .setColor(
                MaterialColors.getColor(
                    applicationContext,
                    com.google.android.material.R.attr.colorPrimary,
                    0,
                ),
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        val total = files.size
        var processed = 0
        builder.setProgress(total, processed, false)
            .setContentText(
                applicationContext.getString(
                    R.string.cleanup_progress,
                    processed,
                    total,
                ),
            )
        setProgress(workDataOf(KEY_PROGRESS_CURRENT to processed, KEY_PROGRESS_TOTAL to total))

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } else {
            Log.w(TAG, "Notification permission not granted")
        }

        val failedPaths = mutableListOf<String>()
        var successCount = 0
        for (file in files) {
            if (isStopped) {
                if (hasPermission) {
                    builder.setProgress(0, 0, false)
                        .setContentTitle(applicationContext.getString(R.string.cleanup_cancelled))
                        .setContentText(applicationContext.getString(R.string.cleanup_cancelled))
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    notificationManager.cancel(NOTIFICATION_ID)
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }
                CleaningEventBus.notifyCleaned(success = false)
                return Result.failure()
            }

            println("FileCleanupWorker ---> Attempting to $action: ${file.absolutePath}")
            when (val res = performAction(action, listOf(file))) {
                is DataState.Error -> {
                    failedPaths += file.absolutePath
                    val reason = when (val err = res.error) {
                        is Errors.Custom -> err.message
                        else -> err.toString()
                    }
                    println("FileCleanupWorker ---> ERROR deleting ${file.absolutePath} → reason = $reason")
                    Log.w(TAG, "Failed to process ${file.absolutePath}: $reason")
                }
                else -> {
                    successCount++
                    println("FileCleanupWorker ---> Deleted: ${file.absolutePath} → result = success")
                }
            }
            processed++
            builder.setProgress(total, processed, false)
                .setContentText(
                    applicationContext.getString(
                        R.string.cleanup_progress,
                        processed,
                        total,
                    ),
                )
            setProgress(workDataOf(KEY_PROGRESS_CURRENT to processed, KEY_PROGRESS_TOTAL to total))
            if (hasPermission) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }

        if (isStopped) {
            if (hasPermission) {
                builder.setProgress(0, 0, false)
                    .setContentTitle(applicationContext.getString(R.string.cleanup_cancelled))
                    .setContentText(applicationContext.getString(R.string.cleanup_cancelled))
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                notificationManager.cancel(NOTIFICATION_ID)
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
            CleaningEventBus.notifyCleaned(success = false)
            return Result.failure()
        }
        builder.setProgress(0, 0, false)

        val failedCount = failedPaths.size
        println("FileCleanupWorker ---> Deleted $successCount, failed $failedCount")
        val resultData = Data.Builder().apply {
            if (failedPaths.isNotEmpty()) {
                putStringArray(KEY_FAILED_PATHS, failedPaths.toTypedArray())
            }
        }.build()

        return when {
            failedCount == 0 -> {
                CleaningEventBus.notifyCleaned(success = true)
                if (hasPermission) {
                    builder.setContentTitle(applicationContext.getString(R.string.cleanup_finished))
                        .setContentText(applicationContext.getString(R.string.all_clean))
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    notificationManager.cancel(NOTIFICATION_ID)
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }
                Result.success()
            }
            successCount == 0 -> {
                if (hasPermission) {
                    builder.setContentTitle(applicationContext.getString(R.string.cleanup_failed))
                        .setContentText(applicationContext.getString(R.string.cleanup_failed_details))
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    notificationManager.cancel(NOTIFICATION_ID)
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }
                CleaningEventBus.notifyCleaned(success = false)
                Result.failure(resultData)
            }
            else -> {
                if (hasPermission) {
                    builder.setContentTitle(applicationContext.getString(R.string.cleanup_finished))
                        .setContentText(
                            applicationContext.getString(
                                R.string.cleanup_partial,
                                successCount,
                                failedCount,
                            ),
                        )
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    notificationManager.cancel(NOTIFICATION_ID)
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }
                CleaningEventBus.notifyCleaned(success = false)
                Result.success(resultData)
            }
        }
    }

    private suspend fun performAction(action: String, files: List<File>): DataState<Unit, *> {
        val mode = if (action == ACTION_TRASH) {
            DeleteFilesUseCase.Mode.TRASH
        } else {
            DeleteFilesUseCase.Mode.PERMANENT
        }
        return cleaningManager.deleteFiles(files, mode)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.cleaning)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                name,
                NotificationManager.IMPORTANCE_LOW,
            )
            NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_PATHS = "paths"
        const val KEY_ACTION = "action"
        const val KEY_ERROR = "error"
        const val KEY_FAILED_PATHS = "failed_paths"
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val ACTION_DELETE = "delete"
        const val ACTION_TRASH = "trash"

        /**
         * Maximum number of file paths accepted by a single work request.
         * Enqueuing code splits larger lists using this value to stay under
         * WorkManager's Data size limit and within expedited work quotas.
         */
        const val MAX_PATHS_PER_WORKER = 100
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_CHANNEL = "file_cleanup"
        private const val TAG = "FileCleanupWorker"
    }
}