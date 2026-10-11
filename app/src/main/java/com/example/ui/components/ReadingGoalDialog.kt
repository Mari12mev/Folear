package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun NewReadingGoalDialog(
    books: List<BookEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        goalType: String,
        bookId: Long?,
        bookTitle: String?,
        targetPages: Int,
        targetDays: Int
    ) -> Unit
) {
    var goalType by remember { mutableStateOf("FINISH_BOOK_IN_DAYS") } // or "READ_PAGES_IN_DAYS"
    var selectedBook by remember { mutableStateOf<BookEntity?>(books.firstOrNull()) }
    var targetDaysText by remember { mutableStateOf("7") }
    var targetPagesText by remember { mutableStateOf("50") }
    var customTitle by remember { mutableStateOf("") }

    val days = targetDaysText.toIntOrNull() ?: 1
    val pages = if (goalType == "FINISH_BOOK_IN_DAYS") {
        selectedBook?.let { max(1, it.totalPages - it.currentPage + 1) } ?: 50
    } else {
        targetPagesText.toIntOrNull() ?: 50
    }

    // Daily rhythm calculation
    val dailyPace = ceil(pages.toDouble() / max(1, days).toDouble()).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Meta de Leitura",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tipo de Meta",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: Terminar um livro em X dias
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { goalType = "FINISH_BOOK_IN_DAYS" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = goalType == "FINISH_BOOK_IN_DAYS",
                        onClick = { goalType = "FINISH_BOOK_IN_DAYS" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Terminar um livro em X dias",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (goalType == "FINISH_BOOK_IN_DAYS") FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Option 2: Ler X páginas em Y dias
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { goalType = "READ_PAGES_IN_DAYS" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = goalType == "READ_PAGES_IN_DAYS",
                        onClick = { goalType = "READ_PAGES_IN_DAYS" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ler X páginas (ou capítulos) em Y dias",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (goalType == "READ_PAGES_IN_DAYS") FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (goalType == "FINISH_BOOK_IN_DAYS") {
                    if (books.isNotEmpty()) {
                        Text(
                            text = "Selecione o Livro ou HQ:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            items(books) { book ->
                                val isSelected = selectedBook?.id == book.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable { selectedBook = book }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Faltam ${max(0, book.totalPages - book.currentPage)} páginas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Nenhum livro cadastrado. Adicione um livro primeiro!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = targetPagesText,
                        onValueChange = { targetPagesText = it.filter { char -> char.isDigit() } },
                        label = { Text("Meta de páginas a ler") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_pages_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetDaysText,
                    onValueChange = { targetDaysText = it.filter { char -> char.isDigit() } },
                    label = { Text("Prazo em dias") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_days_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Calculated daily rhythm banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ritmo diário necessário",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$dailyPace páginas por dia",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = if (goalType == "FINISH_BOOK_IN_DAYS") {
                        "Terminar ${selectedBook?.title ?: "livro"} em $days dias"
                    } else {
                        "Ler $pages páginas em $days dias"
                    }
                    onConfirm(
                        title,
                        goalType,
                        selectedBook?.id,
                        selectedBook?.title,
                        pages,
                        days
                    )
                },
                enabled = days > 0 && pages > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Criar Meta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
