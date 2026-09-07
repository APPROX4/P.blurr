package com.pblurr.app.data.logging

import com.pblurr.app.domain.model.LogCategory
import com.pblurr.app.domain.model.LogEntry
import com.pblurr.app.domain.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    var isEnabled: Boolean = true

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val maxLogEntries = 1000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(
        level: LogLevel = LogLevel.INFO,
        category: LogCategory = LogCategory.APP,
        tag: String = "P.blurr",
        message: String,
        details: String? = null
    ) {
        if (!isEnabled) return
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            details = details
        )
        synchronized(this) {
            val currentList = _logs.value.toMutableList()
            if (currentList.size >= maxLogEntries) {
                currentList.removeAt(0)
            }
            currentList.add(entry)
            _logs.value = currentList
        }
        val formatted = "[${category.name}][${level.name}] $message ${details ?: ""}"
        when (level) {
            LogLevel.INFO -> android.util.Log.i(tag, formatted)
            LogLevel.DEBUG -> android.util.Log.d(tag, formatted)
            LogLevel.WARN -> android.util.Log.w(tag, formatted)
            LogLevel.ERROR -> android.util.Log.e(tag, formatted)
        }
    }

    fun d(category: LogCategory, message: String, details: String? = null) {
        log(LogLevel.DEBUG, category, "P.blurr", message, details)
    }

    fun i(category: LogCategory, message: String, details: String? = null) {
        log(LogLevel.INFO, category, "P.blurr", message, details)
    }

    fun w(category: LogCategory, message: String, details: String? = null) {
        log(LogLevel.WARN, category, "P.blurr", message, details)
    }

    fun e(category: LogCategory, message: String, details: String? = null) {
        log(LogLevel.ERROR, category, "P.blurr", message, details)
    }

    fun clear() {
        synchronized(this) {
            _logs.value = emptyList()
        }
    }

    fun exportFormattedLogs(): String {
        return synchronized(this) {
            _logs.value.joinToString("\n") { entry ->
                val time = dateFormat.format(Date(entry.timestamp))
                "[$time] [${entry.level.name}] [${entry.category.name}] ${entry.message} ${entry.details ?: ""}"
            }
        }
    }
}
