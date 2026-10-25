package com.bookflow.reader.data.model

data class BookMetadata(
    val uri: String,
    val title: String,
    val totalPages: Int,
    val pageOffset: Int = 0,
    val lastReadPage: Int = 0,
    val lastOpenedTimestamp: Long = System.currentTimeMillis()
)
