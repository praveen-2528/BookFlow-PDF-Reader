package com.bookflow.reader.data.repository

import android.content.Context
import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.BookMetadata
import com.bookflow.reader.data.model.BookNoteMeta
import com.bookflow.reader.data.model.PageAnnotations
import com.bookflow.reader.data.model.QuickRecallItem
import com.bookflow.reader.data.model.UniversalNoteMeta
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BookRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("bookflow_prefs", Context.MODE_PRIVATE)
    private val annotationsDir = File(context.filesDir, "annotations").also { it.mkdirs() }

    // ---- Reading View Preferences ----
    fun getPreferredVerticalScroll(): Boolean = prefs.getBoolean("pref_vertical_scroll", true)
    fun setPreferredVerticalScroll(enabled: Boolean) = prefs.edit().putBoolean("pref_vertical_scroll", enabled).apply()

    fun getPreferredDualPage(): Boolean = prefs.getBoolean("pref_dual_page", false)
    fun setPreferredDualPage(enabled: Boolean) = prefs.edit().putBoolean("pref_dual_page", enabled).apply()

    // ---- Book Metadata ----

    fun saveBookMetadata(book: BookMetadata) {
        val booksJson = prefs.getString("books", "[]") ?: "[]"
        val booksArray = JSONArray(booksJson)
        
        // Remove existing entry for same URI
        val filtered = JSONArray()
        for (i in 0 until booksArray.length()) {
            val obj = booksArray.getJSONObject(i)
            if (obj.getString("uri") != book.uri) {
                filtered.put(obj)
            }
        }
        
        // Add updated entry
        val bookJson = JSONObject().apply {
            put("uri", book.uri)
            put("title", book.title)
            put("totalPages", book.totalPages)
            put("pageOffset", book.pageOffset)
            put("lastReadPage", book.lastReadPage)
            put("lastOpenedTimestamp", book.lastOpenedTimestamp)
        }
        filtered.put(bookJson)
        
        prefs.edit().putString("books", filtered.toString()).apply()
    }

    fun getRecentBooks(): List<BookMetadata> {
        val booksJson = prefs.getString("books", "[]") ?: "[]"
        val booksArray = JSONArray(booksJson)
        val books = mutableListOf<BookMetadata>()
        
        for (i in 0 until booksArray.length()) {
            val obj = booksArray.getJSONObject(i)
            books.add(
                BookMetadata(
                    uri = obj.getString("uri"),
                    title = obj.getString("title"),
                    totalPages = obj.getInt("totalPages"),
                    pageOffset = obj.optInt("pageOffset", 0),
                    lastReadPage = obj.optInt("lastReadPage", 0),
                    lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", 0L)
                )
            )
        }
        
        return books.sortedByDescending { it.lastOpenedTimestamp }
    }

    fun getBookMetadata(uri: String): BookMetadata? {
        return getRecentBooks().find { it.uri == uri }
    }

    fun updateBookTitle(uri: String, newTitle: String) {
        val book = getBookMetadata(uri)
        if (book != null) {
            saveBookMetadata(book.copy(title = newTitle))
        }
        renameBookNote(uri, newTitle)
    }

    fun removeBook(uri: String) {
        val booksJson = prefs.getString("books", "[]") ?: "[]"
        val booksArray = JSONArray(booksJson)
        val filtered = JSONArray()
        for (i in 0 until booksArray.length()) {
            val obj = booksArray.getJSONObject(i)
            if (obj.getString("uri") != uri) {
                filtered.put(obj)
            }
        }
        prefs.edit().putString("books", filtered.toString()).apply()
    }

    fun getAllAnnotations(bookUri: String): List<PageAnnotations> {
        val bookDir = File(annotationsDir, bookUri.hashCode().toString())
        if (!bookDir.exists() || !bookDir.isDirectory) return emptyList()
        val files = bookDir.listFiles { _, name -> name.startsWith("page_") && name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            val pageIndex = file.nameWithoutExtension.removePrefix("page_").toIntOrNull() ?: return@mapNotNull null
            loadAnnotations(bookUri, pageIndex)
        }.filter { it.strokes.isNotEmpty() }.sortedBy { it.pageIndex }
    }

    // ---- Bookmarks ----

    fun saveBookmark(bookUri: String, page: Int) {
        val key = "bookmarks_${bookUri.hashCode()}"
        val bookmarks = getBookmarks(bookUri).toMutableSet()
        bookmarks.add(page)
        prefs.edit().putStringSet(key, bookmarks.map { it.toString() }.toSet()).apply()
    }

    fun removeBookmark(bookUri: String, page: Int) {
        val key = "bookmarks_${bookUri.hashCode()}"
        val bookmarks = getBookmarks(bookUri).toMutableSet()
        bookmarks.remove(page)
        prefs.edit().putStringSet(key, bookmarks.map { it.toString() }.toSet()).apply()
    }

    fun getBookmarks(bookUri: String): Set<Int> {
        val key = "bookmarks_${bookUri.hashCode()}"
        return prefs.getStringSet(key, emptySet())?.map { it.toInt() }?.toSet() ?: emptySet()
    }

    fun isBookmarked(bookUri: String, page: Int): Boolean {
        return page in getBookmarks(bookUri)
    }

    // ---- Annotations ----

    fun saveAnnotations(bookUri: String, pageAnnotations: PageAnnotations) {
        val bookDir = File(annotationsDir, bookUri.hashCode().toString()).also { it.mkdirs() }
        val file = File(bookDir, "page_${pageAnnotations.pageIndex}.json")
        
        val strokesArray = JSONArray()
        for (stroke in pageAnnotations.strokes) {
            val pointsArray = JSONArray()
            for (point in stroke.points) {
                pointsArray.put(JSONObject().apply {
                    put("x", point.x.toDouble())
                    put("y", point.y.toDouble()); put("pressure", point.pressure.toDouble())
                })
            }
            strokesArray.put(JSONObject().apply {
                put("points", pointsArray)
                put("color", stroke.color)
                put("strokeWidth", stroke.strokeWidth.toDouble())
                put("isHighlighter", stroke.isHighlighter); put("penType", stroke.penType)
            })
        }
        
        val json = JSONObject().apply {
            put("pageIndex", pageAnnotations.pageIndex)
            put("strokes", strokesArray)
        }
        
        file.writeText(json.toString())
    }

    fun loadAnnotations(bookUri: String, pageIndex: Int): PageAnnotations {
        val bookDir = File(annotationsDir, bookUri.hashCode().toString())
        val file = File(bookDir, "page_$pageIndex.json")
        
        if (!file.exists()) return PageAnnotations(pageIndex)
        
        return try {
            val json = JSONObject(file.readText())
            val strokesArray = json.getJSONArray("strokes")
            val strokes = mutableListOf<AnnotationStroke>()
            
            for (i in 0 until strokesArray.length()) {
                val strokeObj = strokesArray.getJSONObject(i)
                val pointsArray = strokeObj.getJSONArray("points")
                val points = mutableListOf<AnnotationPoint>()
                
                for (j in 0 until pointsArray.length()) {
                    val pointObj = pointsArray.getJSONObject(j)
                    points.add(AnnotationPoint(
                        x = pointObj.getDouble("x").toFloat(),
                        y = pointObj.getDouble("y").toFloat(), pressure = pointObj.optDouble("pressure", 1.0).toFloat()
                    ))
                }
                
                strokes.add(AnnotationStroke(
                    points = points,
                    color = strokeObj.getLong("color"),
                    strokeWidth = strokeObj.getDouble("strokeWidth").toFloat(),
                    isHighlighter = strokeObj.optBoolean("isHighlighter", false), penType = strokeObj.optInt("penType", 0)
                ))
            }
            
            PageAnnotations(pageIndex, strokes)
        } catch (e: Exception) {
            PageAnnotations(pageIndex)
        }
    }

    // ---- Notebook Blocks ----
    fun saveNotebookBlocks(bookUri: String, blocks: List<com.bookflow.reader.data.model.NoteBlock>) {
        val file = File(annotationsDir, "notebook_blocks_${bookUri.hashCode()}.json")
        val arr = JSONArray()
        for (block in blocks) {
            val obj = JSONObject()
            obj.put("id", block.id)
            obj.put("isText", block.isText)
            obj.put("textContent", block.textContent)
            
            val strokesArr = JSONArray()
            for (stroke in block.strokes) {
                val ptsArr = JSONArray()
                for (point in stroke.points) {
                    ptsArr.put(JSONObject().apply { put("x", point.x.toDouble()); put("y", point.y.toDouble()); put("pressure", point.pressure.toDouble()); put("pressure", point.pressure.toDouble()) })
                }
                strokesArr.put(JSONObject().apply {
                    put("points", ptsArr)
                    put("color", stroke.color)
                    put("strokeWidth", stroke.strokeWidth.toDouble())
                    put("isHighlighter", stroke.isHighlighter); put("penType", stroke.penType)
                })
            }
            obj.put("strokes", strokesArr)
            arr.put(obj)
        }
        file.writeText(arr.toString())
    }

    fun loadNotebookBlocks(bookUri: String): List<com.bookflow.reader.data.model.NoteBlock> {
        val file = File(annotationsDir, "notebook_blocks_${bookUri.hashCode()}.json")
        if (!file.exists()) return listOf(com.bookflow.reader.data.model.NoteBlock(id = java.util.UUID.randomUUID().toString(), isText = true))
        return try {
            val arr = JSONArray(file.readText())
            val blocks = mutableListOf<com.bookflow.reader.data.model.NoteBlock>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val isText = obj.getBoolean("isText")
                val strokes = mutableListOf<AnnotationStroke>()
                val strokesArr = obj.getJSONArray("strokes")
                for (j in 0 until strokesArr.length()) {
                    val sObj = strokesArr.getJSONObject(j)
                    val pArr = sObj.getJSONArray("points")
                    val pts = mutableListOf<AnnotationPoint>()
                    for (k in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(k)
                        pts.add(AnnotationPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat(), pObj.optDouble("pressure", 1.0).toFloat()))
                    }
                    strokes.add(AnnotationStroke(
                        points = pts,
                        color = sObj.getLong("color"),
                        strokeWidth = sObj.getDouble("strokeWidth").toFloat(),
                        isHighlighter = sObj.optBoolean("isHighlighter", false), penType = sObj.optInt("penType", 0)
                    ))
                }
                blocks.add(com.bookflow.reader.data.model.NoteBlock(
                    id = obj.getString("id"),
                    isText = isText,
                    textContent = obj.getString("textContent"),
                    strokes = strokes
                ))
            }
            blocks
        } catch (e: Exception) {
            listOf(com.bookflow.reader.data.model.NoteBlock(id = java.util.UUID.randomUUID().toString(), isText = true))
        }
    }

    // ---- Infinite Notes ----

    fun saveInfiniteNotes(bookUri: String, strokes: List<AnnotationStroke>, bookTitle: String? = null) {
        val bookDir = java.io.File(annotationsDir, bookUri.hashCode().toString()).also { it.mkdirs() }
        val file = java.io.File(bookDir, "infinite_notes.json")
        val strokesArray = org.json.JSONArray()
        for (stroke in strokes) {
            val pointsArray = org.json.JSONArray()
            for (point in stroke.points) {
                pointsArray.put(org.json.JSONObject().apply {
                    put("x", point.x.toDouble())
                    put("y", point.y.toDouble())
                    put("pressure", point.pressure.toDouble())
                })
            }
            strokesArray.put(org.json.JSONObject().apply {
                put("points", pointsArray)
                put("color", stroke.color)
                put("strokeWidth", stroke.strokeWidth.toDouble())
                put("isHighlighter", stroke.isHighlighter)
                put("penType", stroke.penType)
                put("pageIndex", stroke.pageIndex)
            })
        }
        file.writeText(strokesArray.toString())

        // If this is a PDF book (not a universal note internal URI), register/update in book_notes_index
        if (!bookUri.startsWith("universal_note_") && bookUri != GLOBAL_NOTEBOOK_URI) {
            val title = bookTitle ?: getBookMetadata(bookUri)?.title ?: extractTitleFromUri(bookUri)
            updateBookNotesRegistry(bookUri, title, strokes.size)
        }
    }

    private fun extractTitleFromUri(uriString: String): String {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val path = uri.path ?: uriString
            val name = path.substringAfterLast('/').substringBeforeLast('.')
            if (name.isNotBlank()) name else "PDF Notes"
        } catch (e: Exception) {
            "PDF Notes"
        }
    }

    // ---- PDF Book Notes Registry (Preserved Even If PDF Is Deleted) ----

    fun getBookNotes(): List<BookNoteMeta> {
        val file = File(context.filesDir, "book_notes_index.json")
        val registeredMap = mutableMapOf<String, BookNoteMeta>()

        if (file.exists()) {
            try {
                val jsonArr = JSONArray(file.readText())
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val uri = obj.getString("bookUri")
                    val annoCount = obj.optInt("pageAnnotationCount", 0)
                    registeredMap[uri] = BookNoteMeta(
                        id = obj.optString("id", uri.hashCode().toString()),
                        bookUri = uri,
                        bookTitle = obj.optString("bookTitle", "PDF Notes"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        strokeCount = obj.optInt("strokeCount", 0),
                        pageAnnotationCount = annoCount,
                        template = obj.optString("template", "DOT_GRID")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Auto-discover any book notes from recent books or existing annotation folders
        val recent = getRecentBooks()
        for (book in recent) {
            val bookDir = File(annotationsDir, book.uri.hashCode().toString())
            val notesFile = File(bookDir, "infinite_notes.json")
            val annos = getAllAnnotations(book.uri)
            val hasNotes = notesFile.exists() && notesFile.length() > 2
            val hasAnnos = annos.isNotEmpty()

            if (hasNotes || hasAnnos) {
                val strokeCount = if (hasNotes) loadInfiniteNotes(book.uri).size else 0
                val existing = registeredMap[book.uri]
                if (existing == null) {
                    registeredMap[book.uri] = BookNoteMeta(
                        id = book.uri.hashCode().toString(),
                        bookUri = book.uri,
                        bookTitle = book.title,
                        updatedAt = if (hasNotes) notesFile.lastModified() else book.lastOpenedTimestamp,
                        strokeCount = strokeCount,
                        pageAnnotationCount = annos.size
                    )
                } else {
                    registeredMap[book.uri] = existing.copy(
                        bookTitle = book.title,
                        strokeCount = strokeCount,
                        pageAnnotationCount = annos.size
                    )
                }
            }
        }

        // Scan annotationsDir for any directories that have infinite_notes.json even if book was removed from recentBooks
        val subDirs = annotationsDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        for (dir in subDirs) {
            val notesFile = File(dir, "infinite_notes.json")
            if (notesFile.exists() && notesFile.length() > 2) {
                val hashStr = dir.name
                val match = registeredMap.values.find { it.bookUri.hashCode().toString() == hashStr }
                if (match == null) {
                    val bookMatch = recent.find { it.uri.hashCode().toString() == hashStr }
                    val strokes = try {
                        val arr = JSONArray(notesFile.readText())
                        arr.length()
                    } catch (e: Exception) { 0 }

                    if (strokes > 0) {
                        val title = bookMatch?.title ?: "PDF Notes (#${hashStr.take(6)})"
                        val pseudoUri = bookMatch?.uri ?: "orphaned_$hashStr"
                        registeredMap[pseudoUri] = BookNoteMeta(
                            id = hashStr,
                            bookUri = pseudoUri,
                            bookTitle = title,
                            updatedAt = notesFile.lastModified(),
                            strokeCount = strokes
                        )
                    }
                }
            }
        }

        val result = registeredMap.values.sortedByDescending { it.updatedAt }
        saveBookNotesRegistry(result)
        return result
    }

    fun saveBookNotesRegistry(notes: List<BookNoteMeta>) {
        val file = File(context.filesDir, "book_notes_index.json")
        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("id", note.id)
                put("bookUri", note.bookUri)
                put("bookTitle", note.bookTitle)
                put("updatedAt", note.updatedAt)
                put("strokeCount", note.strokeCount)
                put("pageAnnotationCount", note.pageAnnotationCount)
                put("template", note.template)
            }
            jsonArray.put(obj)
        }
        file.writeText(jsonArray.toString())
    }

    fun updateBookNotesRegistry(bookUri: String, bookTitle: String, strokeCount: Int) {
        val current = getBookNotes().toMutableList()
        val index = current.indexOfFirst { it.bookUri == bookUri || it.id == bookUri.hashCode().toString() }
        val annos = getAllAnnotations(bookUri)
        val updatedMeta = BookNoteMeta(
            id = bookUri.hashCode().toString(),
            bookUri = bookUri,
            bookTitle = bookTitle,
            updatedAt = System.currentTimeMillis(),
            strokeCount = strokeCount,
            pageAnnotationCount = annos.size
        )
        if (index >= 0) {
            current[index] = updatedMeta
        } else {
            current.add(0, updatedMeta)
        }
        saveBookNotesRegistry(current)
    }

    fun renameBookNote(bookUri: String, newTitle: String) {
        val current = getBookNotes().map {
            if (it.bookUri == bookUri || it.id == bookUri.hashCode().toString()) {
                it.copy(bookTitle = newTitle.trim(), updatedAt = System.currentTimeMillis())
            } else it
        }
        saveBookNotesRegistry(current)
    }

    fun deleteBookNote(bookUri: String) {
        val current = getBookNotes().filterNot { it.bookUri == bookUri || it.id == bookUri.hashCode().toString() }
        saveBookNotesRegistry(current)
        val bookDir = File(annotationsDir, bookUri.hashCode().toString())
        val notesFile = File(bookDir, "infinite_notes.json")
        if (notesFile.exists()) {
            notesFile.delete()
        }
    }

    fun loadInfiniteNotes(bookUri: String): List<AnnotationStroke> {
        val bookDir = java.io.File(annotationsDir, bookUri.hashCode().toString())
        val file = java.io.File(bookDir, "infinite_notes.json")
        if (!file.exists()) return emptyList()
        
        return try {
            val strokesArr = org.json.JSONArray(file.readText())
            val strokes = mutableListOf<AnnotationStroke>()
            for (j in 0 until strokesArr.length()) {
                val sObj = strokesArr.getJSONObject(j)
                val pArr = sObj.getJSONArray("points")
                val pts = mutableListOf<AnnotationPoint>()
                for (k in 0 until pArr.length()) {
                    val pObj = pArr.getJSONObject(k)
                    pts.add(AnnotationPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat(), pObj.optDouble("pressure", 1.0).toFloat()))
                }
                strokes.add(AnnotationStroke(
                    points = pts,
                    color = sObj.getLong("color"),
                    strokeWidth = sObj.getDouble("strokeWidth").toFloat(),
                    isHighlighter = sObj.optBoolean("isHighlighter", false),
                    penType = sObj.optInt("penType", 0),
                    pageIndex = sObj.optInt("pageIndex", 0)
                ))
            }
            strokes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ---- Notebook Image Path Mapping ----

    fun saveNotebookImagePath(hashCode: Long, filePath: String) {
        val key = "img_path_$hashCode"
        prefs.edit().putString(key, filePath).apply()
    }

    fun getNotebookImagePath(hashCode: Long): String? {
        val key = "img_path_$hashCode"
        return prefs.getString(key, null)
    }

    // ---- Sticky Notes ----

    fun saveStickyNotes(bookUri: String, notes: List<com.bookflow.reader.data.model.StickyNote>) {
        try {
            val safeKey = bookUri.hashCode().toString()
            val file = File(annotationsDir, "sticky_$safeKey.json")
            val jsonArray = JSONArray()
            notes.forEach { note ->
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("pageIndex", note.pageIndex)
                    put("x", note.x.toDouble())
                    put("y", note.y.toDouble())
                    put("text", note.text)
                    put("color", note.color)
                    put("isExpanded", note.isExpanded)
                    put("timestamp", note.timestamp)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadStickyNotes(bookUri: String): List<com.bookflow.reader.data.model.StickyNote> {
        return try {
            val safeKey = bookUri.hashCode().toString()
            val file = File(annotationsDir, "sticky_$safeKey.json")
            if (!file.exists()) return emptyList()
            val jsonArray = JSONArray(file.readText())
            val notes = mutableListOf<com.bookflow.reader.data.model.StickyNote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                notes.add(com.bookflow.reader.data.model.StickyNote(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    pageIndex = obj.getInt("pageIndex"),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    text = obj.optString("text", ""),
                    color = obj.optLong("color", 0xFFFFF176),
                    isExpanded = obj.optBoolean("isExpanded", false),
                    timestamp = obj.optLong("timestamp", 0L)
                ))
            }
            notes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ---- Note Text Boxes ----

    fun saveNoteTextBoxes(bookUri: String, textBoxes: List<com.bookflow.reader.data.model.NoteTextBox>) {
        try {
            val safeKey = bookUri.hashCode().toString()
            val file = File(annotationsDir, "textboxes_$safeKey.json")
            val jsonArray = JSONArray()
            textBoxes.forEach { tb ->
                val obj = JSONObject().apply {
                    put("id", tb.id)
                    put("x", tb.x.toDouble())
                    put("y", tb.y.toDouble())
                    put("width", tb.width.toDouble())
                    put("height", tb.height.toDouble())
                    put("text", tb.text)
                    put("fontSize", tb.fontSize.toDouble())
                    put("color", tb.color)
                    put("isBold", tb.isBold)
                    put("isItalic", tb.isItalic)
                    put("isUnderline", tb.isUnderline)
                    put("fontFamily", tb.fontFamily)
                    put("textAlign", tb.textAlign)
                    put("rotation", tb.rotation.toDouble())
                    put("pageIndex", tb.pageIndex)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNoteTextBoxes(bookUri: String): List<com.bookflow.reader.data.model.NoteTextBox> {
        return try {
            val safeKey = bookUri.hashCode().toString()
            val file = File(annotationsDir, "textboxes_$safeKey.json")
            if (!file.exists()) return emptyList()
            val jsonArray = JSONArray(file.readText())
            val boxes = mutableListOf<com.bookflow.reader.data.model.NoteTextBox>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                boxes.add(com.bookflow.reader.data.model.NoteTextBox(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    pageIndex = obj.optInt("pageIndex", 0),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    width = obj.optDouble("width", 300.0).toFloat(),
                    height = obj.optDouble("height", 100.0).toFloat(),
                    text = obj.optString("text", ""),
                    fontSize = obj.optDouble("fontSize", 18.0).toFloat(),
                    color = obj.optLong("color", 0xFF2D2D2D),
                    isBold = obj.optBoolean("isBold", false),
                    isItalic = obj.optBoolean("isItalic", false),
                    isUnderline = obj.optBoolean("isUnderline", false),
                    fontFamily = obj.optString("fontFamily", "DEFAULT"),
                    textAlign = obj.optString("textAlign", "LEFT"),
                    rotation = obj.optDouble("rotation", 0.0).toFloat()
                ))
            }
            boxes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ---- Reading Statistics ----

    fun saveReadingSession(bookUri: String, startTime: Long, endTime: Long, startPage: Int, endPage: Int) {
        val key = "reading_sessions"
        val sessionsJson = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(sessionsJson)
        arr.put(JSONObject().apply {
            put("bookUri", bookUri)
            put("startTime", startTime)
            put("endTime", endTime)
            put("startPage", startPage)
            put("endPage", endPage)
        })
        // Keep last 500 sessions max
        val trimmed = if (arr.length() > 500) {
            val newArr = JSONArray()
            for (i in (arr.length() - 500) until arr.length()) {
                newArr.put(arr.getJSONObject(i))
            }
            newArr
        } else arr
        prefs.edit().putString(key, trimmed.toString()).apply()
    }

    fun getReadingStats(): com.bookflow.reader.ui.components.ReadingStats {
        val key = "reading_sessions"
        val sessionsJson = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(sessionsJson)
        
        val sessions = mutableListOf<com.bookflow.reader.ui.components.ReadingSession>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            sessions.add(com.bookflow.reader.ui.components.ReadingSession(
                bookUri = obj.optString("bookUri", ""),
                startTime = obj.optLong("startTime", 0),
                endTime = obj.optLong("endTime", 0),
                startPage = obj.optInt("startPage", 0),
                endPage = obj.optInt("endPage", 0)
            ))
        }
        
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val totalPages = sessions.sumOf { it.pagesRead }
        val todayMinutes = sessions.filter { it.startTime >= todayStart }.sumOf { it.durationMinutes }
        
        // Weekly minutes (Mon=0 ... Sun=6)
        val weeklyMinutes = MutableList(7) { 0 }
        cal.timeInMillis = now
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis
        
        sessions.filter { it.startTime >= weekStart }.forEach { s ->
            val sCal = java.util.Calendar.getInstance()
            sCal.timeInMillis = s.startTime
            val dayOfWeek = (sCal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
            weeklyMinutes[dayOfWeek] += s.durationMinutes
        }
        
        // Calculate streak (consecutive days with reading)
        val readingDays = sessions.map { s ->
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = s.startTime
            c.set(java.util.Calendar.HOUR_OF_DAY, 0)
            c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0)
            c.set(java.util.Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.toSet().sortedDescending()
        
        var streak = 0
        var expectedDay = todayStart
        for (day in readingDays) {
            if (day == expectedDay || day == expectedDay - 86400000L) {
                streak++
                expectedDay = day - 86400000L
            } else if (day < expectedDay) {
                break
            }
        }
        
        return com.bookflow.reader.ui.components.ReadingStats(
            totalReadingMinutes = totalMinutes,
            totalPagesRead = totalPages,
            totalSessions = sessions.size,
            currentStreak = streak,
            longestStreak = streak,
            todayMinutes = todayMinutes,
            weeklyMinutes = weeklyMinutes,
            recentSessions = sessions.takeLast(10).reversed()
        )
    }

    companion object {
        const val GLOBAL_NOTEBOOK_URI = "global://quick_scratchpad"
        const val GLOBAL_NOTEBOOK_TITLE = "Universal Quick Notebook"
    }

    // ---- Quick Recall Items ----

    fun saveQuickRecallItem(item: QuickRecallItem) {
        val items = getAllQuickRecallItems().toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items[index] = item
        } else {
            items.add(0, item) // Add newest at top
        }
        saveAllQuickRecallItems(items)
    }

    fun deleteQuickRecallItem(id: String) {
        val items = getAllQuickRecallItems().filter { it.id != id }
        saveAllQuickRecallItems(items)
    }

    fun getAllQuickRecallItems(): List<QuickRecallItem> {
        val file = File(context.filesDir, "quick_recall_items.json")
        if (!file.exists()) return emptyList()
        return try {
            val jsonArray = JSONArray(file.readText())
            val list = mutableListOf<QuickRecallItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rectStr = obj.optString("cropRect", "")
                val cropRect = if (rectStr.isNotBlank()) {
                    rectStr.split(",").mapNotNull { it.toFloatOrNull() }.takeIf { it.size == 4 }
                } else null

                list.add(
                    QuickRecallItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        bookUri = obj.optString("bookUri", ""),
                        bookTitle = obj.optString("bookTitle", ""),
                        pageIndex = obj.optInt("pageIndex", 0),
                        tagKey = obj.optString("tagKey", "recall"),
                        emoji = obj.optString("emoji", "🧠"),
                        label = obj.optString("label", "Must Recall"),
                        description = obj.optString("description", ""),
                        imagePath = if (obj.has("imagePath") && !obj.isNull("imagePath")) obj.getString("imagePath") else null,
                        cropRect = cropRect,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getQuickRecallItemsForBook(bookUri: String): List<QuickRecallItem> {
        return getAllQuickRecallItems().filter { it.bookUri == bookUri }
    }

    private fun saveAllQuickRecallItems(items: List<QuickRecallItem>) {
        val file = File(context.filesDir, "quick_recall_items.json")
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("bookUri", item.bookUri)
                put("bookTitle", item.bookTitle)
                put("pageIndex", item.pageIndex)
                put("tagKey", item.tagKey)
                put("emoji", item.emoji)
                put("label", item.label)
                put("description", item.description)
                put("imagePath", item.imagePath ?: "")
                if (item.cropRect != null && item.cropRect.size == 4) {
                    put("cropRect", item.cropRect.joinToString(","))
                }
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        file.writeText(jsonArray.toString())
    }

    // ---- Universal / Global Notebook ----

    fun loadGlobalInfiniteNotes(): List<AnnotationStroke> {
        return loadInfiniteNotes(GLOBAL_NOTEBOOK_URI)
    }

    fun saveGlobalInfiniteNotes(strokes: List<AnnotationStroke>) {
        saveInfiniteNotes(GLOBAL_NOTEBOOK_URI, strokes)
    }

    // ---- Multi-Note Universal Notebook Management ----

    fun getUniversalNotes(): List<UniversalNoteMeta> {
        val file = File(context.filesDir, "universal_notes_index.json")
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val jsonArray = JSONArray(file.readText())
            val list = mutableListOf<UniversalNoteMeta>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    UniversalNoteMeta(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        strokeCount = obj.optInt("strokeCount", 0),
                        template = obj.optString("template", "DOT_GRID")
                    )
                )
            }
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Resets all notes, canvas ink strokes, book note registries, and quick recall items.
     * Leaves reading list untouched or clean.
     */
    fun resetAllNotes() {
        val uFile = File(context.filesDir, "universal_notes_index.json")
        if (uFile.exists()) uFile.delete()

        val bFile = File(context.filesDir, "book_notes_index.json")
        if (bFile.exists()) bFile.delete()

        if (annotationsDir.exists()) {
            annotationsDir.deleteRecursively()
            annotationsDir.mkdirs()
        }

        val qFile = File(context.filesDir, "quick_recall_index.json")
        if (qFile.exists()) qFile.delete()

        val cropsDir = File(context.filesDir, "recall_crops")
        if (cropsDir.exists()) cropsDir.deleteRecursively()
    }

    /**
     * Completely resets the entire app: all notes, library entries, and settings.
     */
    fun resetAppCompletely() {
        resetAllNotes()
        prefs.edit().clear().apply()
    }

    fun saveUniversalNotesIndex(notes: List<UniversalNoteMeta>) {
        val file = File(context.filesDir, "universal_notes_index.json")
        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("updatedAt", note.updatedAt)
                put("strokeCount", note.strokeCount)
                put("template", note.template)
            }
            jsonArray.put(obj)
        }
        file.writeText(jsonArray.toString())
    }

    fun createUniversalNote(title: String = "Untitled Note", template: String = "DOT_GRID"): UniversalNoteMeta {
        val id = java.util.UUID.randomUUID().toString()
        val newNote = UniversalNoteMeta(
            id = id,
            title = if (title.isBlank()) "Untitled Note" else title.trim(),
            updatedAt = System.currentTimeMillis(),
            strokeCount = 0,
            template = template
        )
        val currentNotes = getUniversalNotes().toMutableList()
        currentNotes.add(0, newNote)
        saveUniversalNotesIndex(currentNotes)
        saveInfiniteNotes("universal_note_$id", emptyList())
        return newNote
    }

    fun renameUniversalNote(noteId: String, newTitle: String) {
        val current = getUniversalNotes().map {
            if (it.id == noteId) it.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()) else it
        }
        saveUniversalNotesIndex(current)
    }

    fun deleteUniversalNote(noteId: String) {
        val current = getUniversalNotes().filterNot { it.id == noteId }
        saveUniversalNotesIndex(current)
        val noteFile = File(File(annotationsDir, "universal_note_$noteId".hashCode().toString()), "infinite_notes.json")
        if (noteFile.exists()) {
            noteFile.delete()
        }
    }

    fun loadUniversalNoteStrokes(noteId: String): List<AnnotationStroke> {
        val strokes = loadInfiniteNotes("universal_note_$noteId")
        if (strokes.isEmpty() && noteId == "default_universal_note") {
            return loadInfiniteNotes(GLOBAL_NOTEBOOK_URI)
        }
        return strokes
    }

    fun saveUniversalNoteStrokes(noteId: String, strokes: List<AnnotationStroke>) {
        saveInfiniteNotes("universal_note_$noteId", strokes)
        if (noteId == "default_universal_note") {
            saveInfiniteNotes(GLOBAL_NOTEBOOK_URI, strokes)
        }
        val current = getUniversalNotes().map {
            if (it.id == noteId) it.copy(updatedAt = System.currentTimeMillis(), strokeCount = strokes.size) else it
        }
        saveUniversalNotesIndex(current)
    }

    fun loadUniversalNoteTextBoxes(noteId: String): List<com.bookflow.reader.data.model.NoteTextBox> {
        val boxes = loadNoteTextBoxes("universal_note_$noteId")
        if (boxes.isEmpty() && noteId == "default_universal_note") {
            return loadNoteTextBoxes(GLOBAL_NOTEBOOK_URI)
        }
        return boxes
    }

    fun saveUniversalNoteTextBoxes(noteId: String, textBoxes: List<com.bookflow.reader.data.model.NoteTextBox>) {
        saveNoteTextBoxes("universal_note_$noteId", textBoxes)
        if (noteId == "default_universal_note") {
            saveNoteTextBoxes(GLOBAL_NOTEBOOK_URI, textBoxes)
        }
    }
}

