package com.bookflow.reader.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bookflow.reader.data.model.BookMetadata
import com.bookflow.reader.data.model.BookNoteMeta
import com.bookflow.reader.data.model.UniversalNoteMeta
import com.bookflow.reader.data.repository.BookRepository
import com.bookflow.reader.ui.components.ReadingStats
import com.bookflow.reader.ui.components.ReadingStatsDashboard
import com.bookflow.reader.ui.theme.DarkBackground
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DarkSurfaceVariant
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.DeepBlueDark
import com.bookflow.reader.ui.theme.DeepBlueLight
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.OceanBlueDark
import com.bookflow.reader.ui.theme.WarmCream
import java.util.Calendar

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { BookRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Tab state: 0 = Universal Notes / Notes Hub (Default), 1 = Books Library
    var selectedHomeTab by remember { mutableStateOf(0) }
    
    // Notes Sub-Filter: 0 = All Notes, 1 = Universal Notes, 2 = PDF Book Notes
    var notesFilter by remember { mutableStateOf(0) }
    
    // Library state
    var recentBooks by remember { mutableStateOf<List<BookMetadata>>(emptyList()) }
    var bookToRename by remember { mutableStateOf<BookMetadata?>(null) }
    var bookToDelete by remember { mutableStateOf<BookMetadata?>(null) }
    var renameText by remember { mutableStateOf("") }
    
    // Notes state
    var universalNotes by remember { mutableStateOf<List<UniversalNoteMeta>>(emptyList()) }
    var bookNotes by remember { mutableStateOf<List<BookNoteMeta>>(emptyList()) }
    var activeUniversalNote by remember { mutableStateOf<UniversalNoteMeta?>(null) }
    var activeBookNote by remember { mutableStateOf<BookNoteMeta?>(null) }
    var noteToRename by remember { mutableStateOf<UniversalNoteMeta?>(null) }
    var noteToDelete by remember { mutableStateOf<UniversalNoteMeta?>(null) }
    var bookNoteToRename by remember { mutableStateOf<BookNoteMeta?>(null) }
    var bookNoteToDelete by remember { mutableStateOf<BookNoteMeta?>(null) }
    var renameNoteText by remember { mutableStateOf("") }
    var renameBookNoteText by remember { mutableStateOf("") }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitleText by remember { mutableStateOf("") }
    var showResetNotesDialog by remember { mutableStateOf(false) }
    
    // Reading Stats state
    var showStats by remember { mutableStateOf(false) }
    var readingStats by remember { mutableStateOf(ReadingStats()) }

    // Active Infinite Notepad State
    var showGlobalNotebook by remember { mutableStateOf(false) }
    var globalNotebookStrokes by remember { mutableStateOf<List<com.bookflow.reader.data.model.AnnotationStroke>>(emptyList()) }
    var globalNoteTextBoxes by remember { mutableStateOf<List<com.bookflow.reader.data.model.NoteTextBox>>(emptyList()) }
    var globalUndoStack by remember { mutableStateOf<List<List<com.bookflow.reader.data.model.AnnotationStroke>>>(emptyList()) }
    var globalRedoStack by remember { mutableStateOf<List<List<com.bookflow.reader.data.model.AnnotationStroke>>>(emptyList()) }

    // Quick Recall State
    var showQuickRecallIndex by remember { mutableStateOf(false) }
    var quickRecallItems by remember { mutableStateOf<List<com.bookflow.reader.data.model.QuickRecallItem>>(emptyList()) }
    var showRecallDialog by remember { mutableStateOf(false) }
    var recallSnippetBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Initial load
    LaunchedEffect(Unit) {
        recentBooks = repository.getRecentBooks()
        universalNotes = repository.getUniversalNotes()
        bookNotes = repository.getBookNotes()
    }

    // Auto-refresh when returning to HomeScreen from floating mode or other apps
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val uNotes = repository.getUniversalNotes()
                    val bNotes = repository.getBookNotes()
                    val rBooks = repository.getRecentBooks()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        universalNotes = uNotes
                        bookNotes = bNotes
                        recentBooks = rBooks
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load active universal note or book note strokes and text boxes
    LaunchedEffect(activeUniversalNote?.id, activeBookNote?.bookUri, showGlobalNotebook) {
        if (showGlobalNotebook) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val strokes = if (activeBookNote != null) {
                    repository.loadInfiniteNotes(activeBookNote!!.bookUri)
                } else {
                    val noteId = activeUniversalNote?.id ?: "default_universal_note"
                    repository.loadUniversalNoteStrokes(noteId)
                }
                val boxes = if (activeBookNote != null) {
                    repository.loadNoteTextBoxes(activeBookNote!!.bookUri)
                } else {
                    val noteId = activeUniversalNote?.id ?: "default_universal_note"
                    repository.loadUniversalNoteTextBoxes(noteId)
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    globalNotebookStrokes = strokes
                    globalNoteTextBoxes = boxes
                    globalUndoStack = emptyList()
                    globalRedoStack = emptyList()
                }
            }
        }
    }

    LaunchedEffect(showQuickRecallIndex) {
        if (showQuickRecallIndex) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val items = repository.getAllQuickRecallItems()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    quickRecallItems = items
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            navController.navigate("setup/${Uri.encode(it.toString())}")
        }
    }

    // Dynamic greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Night owl reading"
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            if (selectedHomeTab == 0) {
                FloatingActionButton(
                    onClick = {
                        newNoteTitleText = "Note ${universalNotes.size + 1}"
                        showNewNoteDialog = true
                    },
                    containerColor = DeepBlue,
                    contentColor = WarmCream,
                    shape = CircleShape,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New Note")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Note", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    containerColor = DeepBlue,
                    contentColor = WarmCream,
                    shape = CircleShape,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PostAdd, contentDescription = "Open PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(DarkBackground, DarkSurface)))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // ==========================================
            // TOP HEADER & GREETING
            // ==========================================
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(DeepBlue, OceanBlue)))
                                .border(1.dp, OceanBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = WarmCream,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "BookFlow",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = OceanBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting, Scholar",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                        color = WarmCream
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Recall Index Pill
                    Surface(
                        onClick = { showQuickRecallIndex = true },
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurfaceVariant.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = "Recall", tint = OceanBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Recall",
                                color = WarmCream,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Reading Stats Pill
                    Surface(
                        onClick = {
                            readingStats = repository.getReadingStats()
                            showStats = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurfaceVariant.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, LightMint.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Leaderboard,
                                contentDescription = "Stats",
                                tint = LightMint,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Stats",
                                color = LightMint,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SEGMENTED TAB SWITCHER (Notes Hub vs Books Library)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Universal Notes (Default)
                val totalNotesCount = universalNotes.size + bookNotes.size
                Surface(
                    onClick = { selectedHomeTab = 0 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedHomeTab == 0) DeepBlue else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = if (selectedHomeTab == 0) WarmCream else OceanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Universal Notes",
                            color = if (selectedHomeTab == 0) WarmCream else WarmCream.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (totalNotesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selectedHomeTab == 0) DeepBlueLight else DarkSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalNotesCount",
                                    fontSize = 11.sp,
                                    color = WarmCream,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Tab 1: Books Library
                Surface(
                    onClick = { selectedHomeTab = 1 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedHomeTab == 1) DeepBlue else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (selectedHomeTab == 1) WarmCream else OceanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Books Library",
                            color = if (selectedHomeTab == 1) WarmCream else WarmCream.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (recentBooks.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selectedHomeTab == 1) DeepBlueLight else DarkSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${recentBooks.size}",
                                    fontSize = 11.sp,
                                    color = WarmCream,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // TAB 0: UNIVERSAL & BOOK NOTES HUB
            // ==========================================
            if (selectedHomeTab == 0) {
                val totalNotesCount = universalNotes.size + bookNotes.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notes Hub",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = WarmCream
                        )
                        Text(
                            text = "Universal notebooks & PDF book notes preserved in one place",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightMint.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset All Notes button (only shown when there are notes)
                        if (totalNotesCount > 0) {
                            Surface(
                                onClick = { showResetNotesDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFF790D16).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Reset Notes", tint = Color(0xFFE5D3AF), modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "Reset All",
                                        color = Color(0xFFE5D3AF),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        // Standout + New Note Button
                        Surface(
                            onClick = {
                                newNoteTitleText = "Note ${universalNotes.size + 1}"
                                showNewNoteDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = DeepBlue,
                            border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "New Note", tint = WarmCream, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "New Note",
                                    color = WarmCream,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sub-Filter Chips: All / Universal / PDF Book Notes
                if (totalNotesCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Filter 0: All
                        Surface(
                            onClick = { notesFilter = 0 },
                            shape = RoundedCornerShape(20.dp),
                            color = if (notesFilter == 0) OceanBlue.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (notesFilter == 0) OceanBlue else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "All ($totalNotesCount)",
                                color = if (notesFilter == 0) WarmCream else WarmCream.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (notesFilter == 0) FontWeight.Bold else FontWeight.Normal),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Filter 1: Universal Notes
                        Surface(
                            onClick = { notesFilter = 1 },
                            shape = RoundedCornerShape(20.dp),
                            color = if (notesFilter == 1) OceanBlue.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (notesFilter == 1) OceanBlue else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "Universal (${universalNotes.size})",
                                color = if (notesFilter == 1) WarmCream else WarmCream.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (notesFilter == 1) FontWeight.Bold else FontWeight.Normal),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Filter 2: PDF Book Notes
                        Surface(
                            onClick = { notesFilter = 2 },
                            shape = RoundedCornerShape(20.dp),
                            color = if (notesFilter == 2) Color(0xFF790D16).copy(alpha = 0.4f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (notesFilter == 2) Color(0xFFE5D3AF) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "PDF Book Notes (${bookNotes.size})",
                                color = if (notesFilter == 2) WarmCream else WarmCream.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (notesFilter == 2) FontWeight.Bold else FontWeight.Normal),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // List or Empty State based on filter
                val showUniversal = notesFilter == 0 || notesFilter == 1
                val showBookNotes = notesFilter == 0 || notesFilter == 2
                val hasAnyToShow = (showUniversal && universalNotes.isNotEmpty()) || (showBookNotes && bookNotes.isNotEmpty())

                if (hasAnyToShow) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Section: Universal Notebooks
                        if (showUniversal && universalNotes.isNotEmpty()) {
                            if (notesFilter == 0 && bookNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Universal Notebooks",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = OceanBlue,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                            }
                            items(universalNotes, key = { "univ_${it.id}" }) { note ->
                                UniversalNoteCard(
                                    note = note,
                                    onClick = {
                                        activeUniversalNote = note
                                        activeBookNote = null
                                        showGlobalNotebook = true
                                    },
                                    onRename = {
                                        noteToRename = note
                                        renameNoteText = note.title
                                    },
                                    onDelete = {
                                        noteToDelete = note
                                    }
                                )
                            }
                        }

                        // Section: PDF Book Notes
                        if (showBookNotes && bookNotes.isNotEmpty()) {
                            if (notesFilter == 0 && universalNotes.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PDF Book Notes",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = WarmCream
                                        )
                                        Text(
                                            text = "Preserved permanently",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LightMint.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            items(bookNotes, key = { "booknote_${it.bookUri}" }) { bNote ->
                                PdfBookNoteCard(
                                    note = bNote,
                                    onClick = {
                                        activeBookNote = bNote
                                        activeUniversalNote = null
                                        showGlobalNotebook = true
                                    },
                                    onRename = {
                                        bookNoteToRename = bNote
                                        renameBookNoteText = bNote.bookTitle
                                    },
                                    onDelete = {
                                        bookNoteToDelete = bNote
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Empty Notes State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, OceanBlue.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DriveFileRenameOutline,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Text(
                                text = if (notesFilter == 2) "No PDF Book Notes yet" else "No Notes yet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = WarmCream
                            )

                            Text(
                                text = if (notesFilter == 2)
                                    "When you take infinite notes inside any PDF book,\nthey are automatically preserved here forever, even if the PDF is removed."
                                else
                                    "Create infinite canvas notebooks for freeform notes,\nor write notes in any PDF to have them saved here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightMint.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    newNoteTitleText = "Note ${universalNotes.size + 1}"
                                    showNewNoteDialog = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = WarmCream)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Your First Note", fontWeight = FontWeight.Bold, color = WarmCream)
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // TAB 1: BOOKS LIBRARY
                // ==========================================
                if (recentBooks.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Library",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = WarmCream
                        )
                        Text(
                            text = "${recentBooks.size} ${if (recentBooks.size == 1) "book" else "books"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentBooks, key = { it.uri }) { book ->
                            BookCard(
                                book = book,
                                onClick = {
                                    navController.navigate("reader/${Uri.encode(book.uri)}/${book.pageOffset}")
                                },
                                onRename = {
                                    bookToRename = book
                                    renameText = book.title
                                },
                                onDelete = {
                                    bookToDelete = book
                                }
                            )
                        }
                    }
                } else {
                    // Empty Library State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, OceanBlue.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Text(
                                text = "Your library is ready",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = WarmCream
                            )

                            Text(
                                text = "Open any PDF textbook, paper, or book to start\nannotating with two-page tablet spread.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightMint.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.PostAdd, contentDescription = null, tint = WarmCream)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open your first PDF", fontWeight = FontWeight.Bold, color = WarmCream)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: RESET ALL NOTES
    // ==========================================
    if (showResetNotesDialog) {
        AlertDialog(
            onDismissRequest = { showResetNotesDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Reset All Notes?", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will delete all Universal Notes, PDF Book Notes, and annotations to start with a fresh app. Your PDF files on disk are safe and will not be touched.",
                    color = LightMint.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetAllNotes()
                        universalNotes = emptyList()
                        bookNotes = emptyList()
                        quickRecallItems = emptyList()
                        globalNotebookStrokes = emptyList()
                        showGlobalNotebook = false
                        activeUniversalNote = null
                        activeBookNote = null
                        showResetNotesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.bookflow.reader.ui.theme.AnnotationRed)
                ) {
                    Text("Reset All Notes", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetNotesDialog = false }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DIALOG: CREATE NEW UNIVERSAL NOTE
    // ==========================================
    if (showNewNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNewNoteDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Create Universal Note", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Give your new notebook a title:",
                        color = LightMint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newNoteTitleText,
                        onValueChange = { newNoteTitleText = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Physics Lecture, Math Ideas", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WarmCream,
                            unfocusedTextColor = WarmCream,
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newNoteTitleText.isNotBlank()) newNoteTitleText.trim() else "Note ${universalNotes.size + 1}"
                        val created = repository.createUniversalNote(title)
                        universalNotes = repository.getUniversalNotes()
                        activeUniversalNote = created
                        showGlobalNotebook = true
                        showNewNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Create & Open", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteDialog = false }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DIALOG: RENAME UNIVERSAL NOTE
    // ==========================================
    if (noteToRename != null) {
        AlertDialog(
            onDismissRequest = { noteToRename = null },
            containerColor = DarkSurface,
            title = {
                Text("Rename Notebook", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = renameNoteText,
                    onValueChange = { renameNoteText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream,
                        focusedBorderColor = OceanBlue,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val n = noteToRename
                        if (n != null && renameNoteText.isNotBlank()) {
                            repository.renameUniversalNote(n.id, renameNoteText.trim())
                            universalNotes = repository.getUniversalNotes()
                        }
                        noteToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DIALOG: DELETE UNIVERSAL NOTE
    // ==========================================
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text("Delete Notebook?", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${noteToDelete?.title}'? All ink strokes in this notebook will be permanently removed.",
                    color = LightMint.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val n = noteToDelete
                        if (n != null) {
                            repository.deleteUniversalNote(n.id)
                            universalNotes = repository.getUniversalNotes()
                            if (activeUniversalNote?.id == n.id) {
                                showGlobalNotebook = false
                                activeUniversalNote = null
                            }
                        }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.bookflow.reader.ui.theme.AnnotationRed)
                ) {
                    Text("Delete", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // RENAME BOOK DIALOG
    // ==========================================
    if (bookToRename != null) {
        AlertDialog(
            onDismissRequest = { bookToRename = null },
            containerColor = DarkSurface,
            title = {
                Text("Rename Book", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream,
                        focusedBorderColor = OceanBlue,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToRename
                        if (book != null && renameText.isNotBlank()) {
                            repository.updateBookTitle(book.uri, renameText)
                            recentBooks = repository.getRecentBooks()
                        }
                        bookToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DELETE BOOK DIALOG
    // ==========================================
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text("Remove from Library?", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This removes '${bookToDelete?.title}' from your library list. Any notes and annotations you wrote for this book will remain safely preserved in your Notes page.",
                    color = LightMint.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToDelete
                        if (book != null) {
                            repository.removeBook(book.uri)
                            recentBooks = repository.getRecentBooks()
                            // Refresh bookNotes to ensure orphaned book notes are kept safe and discovered
                            bookNotes = repository.getBookNotes()
                        }
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.bookflow.reader.ui.theme.AnnotationRed)
                ) {
                    Text("Remove", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Keep", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DIALOG: RENAME PDF BOOK NOTE
    // ==========================================
    if (bookNoteToRename != null) {
        AlertDialog(
            onDismissRequest = { bookNoteToRename = null },
            containerColor = DarkSurface,
            title = {
                Text("Rename Book Note", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = renameBookNoteText,
                    onValueChange = { renameBookNoteText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream,
                        focusedBorderColor = OceanBlue,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = bookNoteToRename
                        if (note != null && renameBookNoteText.isNotBlank()) {
                            repository.renameBookNote(note.bookUri, renameBookNoteText.trim())
                            bookNotes = repository.getBookNotes()
                        }
                        bookNoteToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookNoteToRename = null }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // ==========================================
    // DIALOG: DELETE PDF BOOK NOTE
    // ==========================================
    if (bookNoteToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookNoteToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text("Delete Book Notes?", color = WarmCream, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete notes for '${bookNoteToDelete?.bookTitle}'? All written ink strokes for this book will be permanently removed. Your original PDF file will not be affected.",
                    color = LightMint.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = bookNoteToDelete
                        if (note != null) {
                            repository.deleteBookNote(note.bookUri)
                            bookNotes = repository.getBookNotes()
                            if (activeBookNote?.bookUri == note.bookUri) {
                                showGlobalNotebook = false
                                activeBookNote = null
                            }
                        }
                        bookNoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.bookflow.reader.ui.theme.AnnotationRed)
                ) {
                    Text("Delete", color = WarmCream, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookNoteToDelete = null }) {
                    Text("Cancel", color = LightMint)
                }
            }
        )
    }

    // Reading Stats Dashboard Modal
    ReadingStatsDashboard(
        isVisible = showStats,
        stats = readingStats,
        onClose = { showStats = false }
    )

    // ==========================================
    // INFINITE NOTEBOOK CANVAS (Universal or PDF Book Note)
    // ==========================================
    if (showGlobalNotebook) {
        val currentTitle = activeBookNote?.bookTitle ?: activeUniversalNote?.title ?: "Notebook"
        val currentId = activeUniversalNote?.id ?: "default_universal_note"

        val saveCurrentStrokes: (List<com.bookflow.reader.data.model.AnnotationStroke>) -> Unit = { updated ->
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (activeBookNote != null) {
                    repository.saveInfiniteNotes(activeBookNote!!.bookUri, updated, activeBookNote!!.bookTitle)
                    val updatedNotes = repository.getBookNotes()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        bookNotes = updatedNotes
                    }
                } else {
                    repository.saveUniversalNoteStrokes(currentId, updated)
                    val updatedNotes = repository.getUniversalNotes()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        universalNotes = updatedNotes
                    }
                }
            }
        }

        val saveCurrentTextBoxes: (List<com.bookflow.reader.data.model.NoteTextBox>) -> Unit = { updatedBoxes ->
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (activeBookNote != null) {
                    repository.saveNoteTextBoxes(activeBookNote!!.bookUri, updatedBoxes)
                } else {
                    repository.saveUniversalNoteTextBoxes(currentId, updatedBoxes)
                }
            }
        }

        var eraserGestureSnapshot by remember { mutableStateOf<List<com.bookflow.reader.data.model.AnnotationStroke>?>(null) }

        com.bookflow.reader.ui.components.InfiniteNotePad(
            strokes = globalNotebookStrokes,
            canUndo = globalUndoStack.isNotEmpty(),
            canRedo = globalRedoStack.isNotEmpty(),
            noteTextBoxes = globalNoteTextBoxes,
            onAddTextBox = { x, y ->
                val newBox = com.bookflow.reader.data.model.NoteTextBox(x = x, y = y)
                val updated = globalNoteTextBoxes + newBox
                globalNoteTextBoxes = updated
                saveCurrentTextBoxes(updated)
            },
            onAddTextBoxObj = { newBox ->
                val updated = globalNoteTextBoxes + newBox
                globalNoteTextBoxes = updated
                saveCurrentTextBoxes(updated)
            },
            onUpdateTextBox = { updatedBox ->
                val updated = globalNoteTextBoxes.map { if (it.id == updatedBox.id) updatedBox else it }
                globalNoteTextBoxes = updated
                saveCurrentTextBoxes(updated)
            },
            onDeleteTextBox = { boxId ->
                val updated = globalNoteTextBoxes.filter { it.id != boxId }
                globalNoteTextBoxes = updated
                saveCurrentTextBoxes(updated)
            },
            isGlobalNotebook = true,
            noteTitle = currentTitle,
            initialFullScreen = true,
            onAddStroke = { stroke ->
                globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                globalRedoStack = emptyList()
                val updated = globalNotebookStrokes + stroke
                globalNotebookStrokes = updated
                saveCurrentStrokes(updated)
            },
            onEraseAt = { worldX, worldY ->
                val (updated, changed) = com.bookflow.reader.util.NoteEraserHelper.eraseStrokesAt(
                    globalNotebookStrokes,
                    worldX,
                    worldY,
                    24f,
                    com.bookflow.reader.ui.components.EraserMode.STROKE
                )
                if (changed) {
                    globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                    globalRedoStack = emptyList()
                    globalNotebookStrokes = updated
                    saveCurrentStrokes(updated)
                }
            },
            onEraseStrokeAt = { worldX, worldY, radius, mode, pageIndex ->
                if (eraserGestureSnapshot == null) {
                    eraserGestureSnapshot = globalNotebookStrokes
                }
                val (updated, changed) = com.bookflow.reader.util.NoteEraserHelper.eraseStrokesAt(
                    globalNotebookStrokes,
                    worldX,
                    worldY,
                    radius,
                    mode,
                    pageIndex
                )
                if (changed) {
                    globalNotebookStrokes = updated
                }
            },
            onEraseGestureComplete = {
                val snapshot = eraserGestureSnapshot
                if (snapshot != null) {
                    if (snapshot != globalNotebookStrokes) {
                        globalUndoStack = globalUndoStack + listOf(snapshot)
                        globalRedoStack = emptyList()
                        saveCurrentStrokes(globalNotebookStrokes)
                    }
                    eraserGestureSnapshot = null
                }
            },
            onClear = {
                if (globalNotebookStrokes.isNotEmpty()) {
                    globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                    globalRedoStack = emptyList()
                    globalNotebookStrokes = emptyList()
                    saveCurrentStrokes(emptyList())
                }
            },
            onUndo = {
                if (globalUndoStack.isNotEmpty()) {
                    val last = globalUndoStack.last()
                    globalRedoStack = globalRedoStack + listOf(globalNotebookStrokes)
                    globalUndoStack = globalUndoStack.dropLast(1)
                    globalNotebookStrokes = last
                    saveCurrentStrokes(last)
                }
            },
            onRedo = {
                if (globalRedoStack.isNotEmpty()) {
                    val next = globalRedoStack.last()
                    globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                    globalRedoStack = globalRedoStack.dropLast(1)
                    globalNotebookStrokes = next
                    saveCurrentStrokes(next)
                }
            },
            onExport = {
                val file = com.bookflow.reader.util.PdfExporter.exportNotesToPdf(
                    context = context,
                    strokes = globalNotebookStrokes,
                    imagePaths = emptyMap(),
                    fileName = "${currentTitle.replace(' ', '_')}_${System.currentTimeMillis()}"
                )
                if (file != null) {
                    com.bookflow.reader.util.PdfExporter.sharePdf(context, file)
                }
            },
            onClose = {
                showGlobalNotebook = false
                activeUniversalNote = null
                activeBookNote = null
            },
            onMoveStrokes = { indices, dx, dy ->
                val updated = globalNotebookStrokes.toMutableList()
                indices.forEach { idx ->
                    if (idx in updated.indices) {
                        val s = updated[idx]
                        updated[idx] = s.copy(points = s.points.map { it.copy(x = it.x + dx, y = it.y + dy) })
                    }
                }
                globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                globalRedoStack = emptyList()
                globalNotebookStrokes = updated
                saveCurrentStrokes(updated)
            },
            onScaleStrokes = { indices, scaleFactor, ox, oy ->
                val updated = globalNotebookStrokes.toMutableList()
                indices.forEach { idx ->
                    if (idx in updated.indices) {
                        val s = updated[idx]
                        updated[idx] = s.copy(
                            points = s.points.map { it.copy(x = ox + (it.x - ox) * scaleFactor, y = oy + (it.y - oy) * scaleFactor) },
                            strokeWidth = s.strokeWidth * scaleFactor
                        )
                    }
                }
                globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                globalRedoStack = emptyList()
                globalNotebookStrokes = updated
                saveCurrentStrokes(updated)
            },
            onDeleteStrokes = { indices ->
                val updated = globalNotebookStrokes.filterIndexed { idx, _ -> idx !in indices }
                globalUndoStack = globalUndoStack + listOf(globalNotebookStrokes)
                globalRedoStack = emptyList()
                globalNotebookStrokes = updated
                saveCurrentStrokes(updated)
            },
            onMarkRecallSelection = { bitmap ->
                recallSnippetBitmap = bitmap
                showRecallDialog = true
            },
            onFloatOverApps = {
                val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else true

                if (!hasOverlayPermission) {
                    try {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        android.widget.Toast.makeText(
                            context,
                            "Please grant 'Display over other apps' to float your notebook",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "Could not open overlay settings: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    com.bookflow.reader.service.FloatingNoteData.strokes = globalNotebookStrokes
                    com.bookflow.reader.service.FloatingNoteData.textBoxes = globalNoteTextBoxes
                    com.bookflow.reader.service.FloatingNoteData.canvasTemplate = com.bookflow.reader.data.model.CanvasTemplate.DOT_GRID
                    com.bookflow.reader.service.FloatingNoteData.bookUri = activeBookNote?.bookUri ?: ""
                    com.bookflow.reader.service.FloatingNoteData.bookTitle = currentTitle
                    com.bookflow.reader.service.FloatingNoteData.universalNoteId = if (activeBookNote == null) (activeUniversalNote?.id ?: "default_universal_note") else ""
                    com.bookflow.reader.service.FloatingNoteData.isGlobalMode = activeUniversalNote != null || activeBookNote == null
                    com.bookflow.reader.service.FloatingNoteData.isStylusOnly = true

                    showGlobalNotebook = false
                    activeUniversalNote = null
                    activeBookNote = null

                    com.bookflow.reader.service.FloatingNoteService.start(context)
                    android.widget.Toast.makeText(
                        context,
                        "Floating notebook active! You can now use any other app.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Mark Recall Dialog for floating notes
    if (showRecallDialog) {
        com.bookflow.reader.ui.components.MarkRecallDialog(
            pageIndex = 0,
            bitmap = recallSnippetBitmap,
            onSave = { emoji, label, description, tagKey ->
                val cropsDir = java.io.File(context.filesDir, "recall_crops").also { it.mkdirs() }
                val cropFile = java.io.File(cropsDir, "recall_${System.currentTimeMillis()}.png")
                recallSnippetBitmap?.let { bmp ->
                    cropFile.outputStream().use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                    }
                }
                val currentTitle = activeBookNote?.bookTitle ?: activeUniversalNote?.title ?: "Notebook"
                val item = com.bookflow.reader.data.model.QuickRecallItem(
                    id = java.util.UUID.randomUUID().toString(),
                    bookUri = activeBookNote?.bookUri ?: BookRepository.GLOBAL_NOTEBOOK_URI,
                    bookTitle = currentTitle,
                    pageIndex = 0,
                    emoji = emoji,
                    label = label,
                    description = description,
                    tagKey = tagKey,
                    imagePath = cropFile.absolutePath,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveQuickRecallItem(item)
                quickRecallItems = repository.getAllQuickRecallItems()
                showRecallDialog = false
                recallSnippetBitmap = null
            },
            onDismiss = {
                showRecallDialog = false
                recallSnippetBitmap = null
            }
        )
    }

    // Quick Recall Index & Navigation Panel
    com.bookflow.reader.ui.components.QuickRecallIndexPanel(
        isVisible = showQuickRecallIndex,
        items = quickRecallItems,
        currentBookUri = null,
        onNavigateToItem = { bookUri, pageIndex, cropRect ->
            showQuickRecallIndex = false
            if (bookUri == BookRepository.GLOBAL_NOTEBOOK_URI) {
                showGlobalNotebook = true
            } else {
                val route = if (cropRect != null && cropRect.size >= 4) {
                    "reader/${Uri.encode(bookUri)}/$pageIndex?highlight=${cropRect.joinToString(",")}"
                } else {
                    "reader/${Uri.encode(bookUri)}/$pageIndex"
                }
                navController.navigate(route)
            }
        },
        onDeleteItem = { id ->
            repository.deleteQuickRecallItem(id)
            quickRecallItems = repository.getAllQuickRecallItems()
        },
        onDismiss = { showQuickRecallIndex = false }
    )
}

@Composable
private fun BookCard(
    book: BookMetadata,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Spine Icon Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(DeepBlueDark, DeepBlue, OceanBlue)
                        )
                    )
                    .border(1.dp, OceanBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = WarmCream,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                val total = book.totalPages.coerceAtLeast(1)
                val current = book.lastReadPage.coerceIn(0, total)
                val progressFloat = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val progressPct = (progressFloat * 100).toInt()

                LinearProgressIndicator(
                    progress = { progressFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = OceanBlue,
                    trackColor = DarkSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page $current of $total",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightMint.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$progressPct%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = OceanBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = WarmCream.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = com.bookflow.reader.ui.theme.AnnotationRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalNoteCard(
    note: UniversalNoteMeta,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note Cover Icon Thumbnail with standout gradient
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(DeepBlue, DeepBlueDark, OceanBlueDark)
                        )
                    )
                    .border(1.dp, OceanBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = WarmCream,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stroke count badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepBlueDark.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, DeepBlueLight.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (note.strokeCount > 0) "${note.strokeCount} strokes" else "Empty note",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = OceanBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• Edited ${formatRelativeTime(note.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightMint.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = WarmCream.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = com.bookflow.reader.ui.theme.AnnotationRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfBookNoteCard(
    note: BookNoteMeta,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover / Notebook Spine Thumbnail with Burgundy Palette & PDF Indicator
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF790D16), DeepBlueDark, DeepBlue)
                        )
                    )
                    .border(1.dp, Color(0xFFE5D3AF).copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = WarmCream,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PDF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE5D3AF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.bookTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Distinct PDF Book Note Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF790D16).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF790D16).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "PDF Book Note",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE5D3AF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Stroke count badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepBlueDark.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, DeepBlueLight.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (note.strokeCount > 0) "${note.strokeCount} strokes" else "Empty notes",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = OceanBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• Edited ${formatRelativeTime(note.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightMint.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = WarmCream.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = com.bookflow.reader.ui.theme.AnnotationRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
