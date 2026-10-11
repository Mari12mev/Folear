package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.ui.components.BookCard
import com.example.ui.components.ContinueReadingCard
import com.example.ui.components.getFolderIcon
import com.example.ui.viewmodel.BookViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val books by viewModel.displayedBooks.collectAsState()
    val inProgressBooks by viewModel.inProgressBooks.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showBackupMenu by remember { mutableStateOf(false) }

    // SAF file import launcher for books
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            viewModel.importBook(uri)
        }
    }

    // SAF backup export launcher (JSON)
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri) { success ->
                Toast.makeText(context, if (success) "Backup exportado com sucesso!" else "Falha ao exportar backup.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF backup import launcher (JSON)
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackup(uri) { success ->
                Toast.makeText(context, if (success) "Backup importado com sucesso!" else "Falha ao importar backup.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Folhear",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (isIncognito) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE63946)) {
                                            Text(
                                                "INCÓGNITO",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Livros, Mangás & Quadrinhos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Backup Menu Button
                            Box {
                                IconButton(onClick = { showBackupMenu = true }) {
                                    Icon(Icons.Default.Backup, contentDescription = "Backup", tint = MaterialTheme.colorScheme.onBackground)
                                }
                                DropdownMenu(expanded = showBackupMenu, onDismissRequest = { showBackupMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Exportar Backup (.json)") },
                                        leadingIcon = { Icon(Icons.Default.CloudUpload, null) },
                                        onClick = {
                                            showBackupMenu = false
                                            exportBackupLauncher.launch("folhear_backup_${System.currentTimeMillis()}.json")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Importar Backup (.json)") },
                                        leadingIcon = { Icon(Icons.Default.Backup, null) },
                                        onClick = {
                                            showBackupMenu = false
                                            importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                        }
                                    )
                                }
                            }

                            // Incognito Toggle
                            IconButton(onClick = { viewModel.toggleIncognito() }) {
                                Icon(
                                    imageVector = if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Alternar Modo Incógnito",
                                    tint = if (isIncognito) Color(0xFFE63946) else MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Dark/Light toggle
                            IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Alternar Tema",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Search toggle
                            IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }

                    // Search Input
                    AnimatedVisibility(visible = isSearchExpanded, enter = fadeIn(), exit = fadeOut()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar títulos, autores ou gêneros...") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("search_text_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Reading Status Filters Row: "Todos", "Não lidos", "Em andamento", "Concluídos", "Favoritos"
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusFilterChip("Todos", statusFilter == "ALL") { viewModel.setStatusFilter("ALL") }
                        StatusFilterChip("Não lidos", statusFilter == "UNREAD") { viewModel.setStatusFilter("UNREAD") }
                        StatusFilterChip("Em andamento", statusFilter == "READING") { viewModel.setStatusFilter("READING") }
                        StatusFilterChip("Concluídos", statusFilter == "COMPLETED") { viewModel.setStatusFilter("COMPLETED") }
                        StatusFilterChip("★ Favoritos", statusFilter == "FAVORITES") { viewModel.setStatusFilter("FAVORITES") }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Folders and Tags Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folders Filter
                        for (folder in folders) {
                            val isSel = selectedFolderId == folder.id
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable {
                                    viewModel.selectFolderFilter(if (isSel) null else folder.id)
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Icon(
                                        imageVector = getFolderIcon(folder.iconName),
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colorScheme.onPrimary else try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(folder.name, style = MaterialTheme.typography.labelMedium, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Tags Filter
                        for (tag in tags) {
                            val isSel = selectedTagId == tag.id
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable {
                                    viewModel.selectTagFilter(if (isSel) null else tag.id)
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Icon(Icons.Default.Label, null, tint = if (isSel) MaterialTheme.colorScheme.onPrimary else try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { Color.Gray }, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tag.name, style = MaterialTheme.typography.labelMedium, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // "+ Nova Tag" Chip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { viewModel.showNewTagDialog.value = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nova Tag", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // In Progress Section ("Continuar Lendo")
            if (inProgressBooks.isNotEmpty() && selectedFolderId == null && selectedTagId == null && searchQuery.isEmpty() && statusFilter == "ALL") {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            text = "Continuar Lendo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val topInProgress = inProgressBooks.first()
                        ContinueReadingCard(book = topInProgress, onClick = { viewModel.openReader(topInProgress) })
                    }
                }
            }

            // Library Sorting and Counter Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biblioteca (${books.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Sort Selector
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showSortMenu = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (sortOption) {
                                        "TITLE" -> "Título (A-Z)"
                                        "AUTHOR" -> "Autor"
                                        "DATE_ADDED" -> "Data Adição"
                                        "PROGRESS" -> "Progresso"
                                        else -> "Última Leitura"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Última Leitura") }, onClick = { viewModel.setSortOption("LAST_READ"); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Título (A-Z)") }, onClick = { viewModel.setSortOption("TITLE"); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Autor") }, onClick = { viewModel.setSortOption("AUTHOR"); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Data de Adição") }, onClick = { viewModel.setSortOption("DATE_ADDED"); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Progresso (% lido)") }, onClick = { viewModel.setSortOption("PROGRESS"); showSortMenu = false })
                        }
                    }
                }
            }

            // Book Cards Grid
            if (books.isNotEmpty()) {
                items(books, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        onClick = { viewModel.openReader(book) },
                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                        onToggleCompleted = { viewModel.toggleCompleted(book) },
                        onEditDetails = { viewModel.editingBookDetails.value = book },
                        onManageTags = { viewModel.managingBookTags.value = book },
                        onManageFolders = { viewModel.managingBookFolders.value = book },
                        onChangeCover = { viewModel.changingCoverBook.value = book },
                        onRestoreCover = { viewModel.restoreOriginalCover(book.id) },
                        onDelete = { viewModel.deleteBook(book) }
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoStories, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Nenhum item encontrado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Importe livros e HQs ou ajuste os filtros acima.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Import FAB
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "application/x-cbz", "application/zip", "*/*"))
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).testTag("import_fab")
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isImporting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Importar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isImporting) "Importando..." else "Importar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
