package com.bookflow.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream

@Composable
fun GoToPageDialog(
    currentPage: Int,
    totalPages: Int,
    pageOffset: Int,
    bookmarks: List<Int>,
    onGoToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pageInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Go to Page",
                style = MaterialTheme.typography.headlineMedium,
                color = WarmCream
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Currently on page ${currentPage + 1 + pageOffset} of $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = pageInput,
                onValueChange = { 
                    pageInput = it.filter { c -> c.isDigit() }
                    errorText = null
                },
                label = { Text("Page number (1 - $totalPages)", color = Color.Gray) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val pageNum = pageInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..totalPages) {
                            onGoToPage(pageNum - 1 - pageOffset)
                            onDismiss()
                        } else {
                            errorText = "Enter a number between 1 and $totalPages"
                        }
                    }
                ),
                isError = errorText != null,
                supportingText = if (errorText != null) {{ Text(errorText!!, color = Color.Red) }} else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream,
                    cursorColor = OceanBlue,
                    focusedBorderColor = OceanBlue,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bookmarks section
            if (bookmarks.isNotEmpty()) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleSmall,
                    color = OceanBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bookmarks.take(10).forEach { bookmarkPage ->
                        TextButton(
                            onClick = {
                                onGoToPage(bookmarkPage)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Page ${bookmarkPage + 1 + pageOffset}",
                                    color = WarmCream
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val pageNum = pageInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..totalPages) {
                            onGoToPage(pageNum - 1 - pageOffset)
                            onDismiss()
                        } else {
                            errorText = "Enter a number between 1 and $totalPages"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Go", color = Color.White)
                }
            }
        }
    }
}
