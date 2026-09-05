package com.pblurr.app.presentation.ui.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val logsState by AppLogger.logs.collectAsState()
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logsState.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val logText = remember(logsState) {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        logsState.joinToString("\n") { entry ->
            val levelTag = when (entry.level) {
                LogLevel.INFO  -> "I"
                LogLevel.DEBUG -> "D"
                LogLevel.WARN  -> "W"
                LogLevel.ERROR -> "E"
            }
            val time = fmt.format(Date(entry.timestamp))
            "[$time][$levelTag][${entry.category.name}] ${entry.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Diagnostics Log", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Copy log action button ONLY (Delete option strictly removed)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("P.blurr Logs", logText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied logs to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C0C0E))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
        ) {
            if (logsState.isEmpty()) {
                Text(
                    "No system logs recorded.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = logText,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(12.dp)
                )
            }
        }
    }
}
