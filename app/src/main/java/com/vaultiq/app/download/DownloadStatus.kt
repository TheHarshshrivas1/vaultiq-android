package com.vaultiq.app.download

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromValue(value: String): DownloadStatus {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: FAILED
        }
    }
}
