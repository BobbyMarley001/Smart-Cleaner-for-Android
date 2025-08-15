package com.d4rk.cleaner.core.domain.model.network

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.Error

sealed interface Errors : Error {

    /**
     * Generic error used when a more specific type is not available. It carries
     * the original message so that it can be logged for debugging purposes.
     */
    data class Custom(val message: String? = null) : Errors {
        override fun toString(): String = message ?: "Unknown error"
    }

    enum class Network : Errors {
        REQUEST_TIMEOUT, NO_INTERNET, SERVER_ERROR, SERIALIZATION
    }

    enum class UseCase : Errors {
        NO_DATA, INVALID_DATA, FAILED_TO_GET_INSTALLED_APPS, FAILED_TO_GET_APK_FILES, FAILED_TO_GET_STORAGE_INFO, FAILED_TO_GET_RAM_INFO, FAILED_TO_INSTALL_APK, FAILED_TO_SHARE_APK, FAILED_TO_SHARE_APP, FAILED_TO_OPEN_APP_INFO, FAILED_TO_UNINSTALL_APP, FAILED_TO_GET_APP_USAGE_STATS
    }

    enum class Database : Errors {
        DATABASE_OPERATION_FAILED
    }
}
