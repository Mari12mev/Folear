package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.CoverDialog
import com.example.ui.components.CreateOrEditFolderDialog
import com.example.ui.components.CreateTagDialog
import com.example.ui.components.EditBookDetailsDialog
import com.example.ui.components.ManageBookFoldersDialog
import com.example.ui.components.ManageBookTagsDialog
import com.example.ui.components.NewReadingGoalDialog
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val readingBook by viewModel.readingBook.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                if (readingBook != null) {
                    // Fullscreen immersive reader
                    ReaderScreen(
                        book = readingBook!!,
                        viewModel = viewModel,
                        onBack = { viewModel.closeReader() }
                    )
                } else {
                    // Main App Navigation Shell
                    FolhearMainShell(viewModel = viewModel, currentTab = currentTab)
                }
            }
        }
    }
}

@Composable
fun FolhearMainShell(
    viewModel: BookViewModel,
    currentTab: String
) {
    val showNewFolder by viewModel.showNewFolderDialog.collectAsState()
    val editingFolder by viewModel.editingFolder.collectAsState()
    val managingBookFolders by viewModel.managingBookFolders.collectAsState()
    val showNewTag by viewModel.showNewTagDialog.collectAsState()
    val managingBookTags by viewModel.managingBookTags.collectAsState()
    val editingBookDetails by viewModel.editingBookDetails.collectAsState()
    val changingCoverBook by viewModel.changingCoverBook.collectAsState()
    val showNewGoal by viewModel.showNewGoalDialog.collectAsState()
    val showCbrInfo by viewModel.showCbrInfoDialog.collectAsState()

    val allFolders by viewModel.allFolders.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val displayedBooks by viewModel.displayedBooks.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == "library",
                    onClick = { viewModel.setTab("library") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Biblioteca") },
                    label = { Text("Biblioteca", fontWeight = if (currentTab == "library") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_library")
                )

                NavigationBarItem(
                    selected = currentTab == "folders",
                    onClick = { viewModel.setTab("folders") },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Pastas") },
                    label = { Text("Pastas", fontWeight = if (currentTab == "folders") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_folders")
                )

                NavigationBarItem(
                    selected = currentTab == "goals",
                    onClick = { viewModel.setTab("goals") },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Metas") },
                    label = { Text("Metas", fontWeight = if (currentTab == "goals") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_goals")
                )

                NavigationBarItem(
                    selected = currentTab == "stats",
                    onClick = { viewModel.setTab("stats") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Estatísticas") },
                    label = { Text("Histórico", fontWeight = if (currentTab == "stats") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_stats")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "library" -> LibraryScreen(viewModel = viewModel)
                "folders" -> FoldersScreen(viewModel = viewModel)
                "goals" -> GoalsScreen(viewModel = viewModel)
                "stats" -> StatsScreen(viewModel = viewModel)
            }
        }
    }

    // New Folder Dialog
    if (showNewFolder) {
        CreateOrEditFolderDialog(
            initialFolder = null,
            onDismiss = { viewModel.showNewFolderDialog.value = false },
            onConfirm = { name, icon, colorHex ->
                viewModel.createFolder(name, icon, colorHex)
            }
        )
    }

    // Edit Folder Dialog
    if (editingFolder != null) {
        CreateOrEditFolderDialog(
            initialFolder = editingFolder,
            onDismiss = { viewModel.editingFolder.value = null },
            onConfirm = { name, icon, colorHex ->
                editingFolder?.let {
                    viewModel.updateFolder(it.copy(name = name, iconName = icon, colorHex = colorHex))
                }
            }
        )
    }

    // Manage Book Folders Dialog
    if (managingBookFolders != null) {
        val book = managingBookFolders!!
        var bookFolderIds = emptyList<Long>()
        androidx.compose.runtime.LaunchedEffect(book.id) {
            bookFolderIds = viewModel.getFolderIdsForBook(book.id)
        }

        ManageBookFoldersDialog(
            book = book,
            allFolders = allFolders,
            initialFolderIds = bookFolderIds,
            onDismiss = { viewModel.managingBookFolders.value = null },
            onSave = { ids -> viewModel.setBookFolders(book.id, ids) }
        )
    }

    // New Tag Dialog
    if (showNewTag) {
        CreateTagDialog(
            onDismiss = { viewModel.showNewTagDialog.value = false },
            onConfirm = { name, colorHex -> viewModel.createTag(name, colorHex) }
        )
    }

    // Manage Book Tags Dialog
    if (managingBookTags != null) {
        val book = managingBookTags!!
        var bookTagIds = emptyList<Long>()
        androidx.compose.runtime.LaunchedEffect(book.id) {
            bookTagIds = viewModel.getTagIdsForBook(book.id)
        }

        ManageBookTagsDialog(
            book = book,
            allTags = allTags,
            initialTagIds = bookTagIds,
            onDismiss = { viewModel.managingBookTags.value = null },
            onSave = { ids -> viewModel.setBookTags(book.id, ids) }
        )
    }

    // Edit Book Title & Author Dialog
    if (editingBookDetails != null) {
        val book = editingBookDetails!!
        EditBookDetailsDialog(
            book = book,
            onDismiss = { viewModel.editingBookDetails.value = null },
            onSave = { newTitle, newAuthor -> viewModel.updateBookDetails(book.id, newTitle, newAuthor) }
        )
    }

    // Change Cover Dialog
    if (changingCoverBook != null) {
        val book = changingCoverBook!!
        CoverDialog(
            book = book,
            onDismiss = { viewModel.changingCoverBook.value = null },
            onCoverSelected = { newPath ->
                viewModel.updateCover(book.id, newPath)
                viewModel.changingCoverBook.value = null
            },
            onRestoreOriginal = {
                viewModel.restoreOriginalCover(book.id)
                viewModel.changingCoverBook.value = null
            }
        )
    }

    // New Reading Goal Dialog
    if (showNewGoal) {
        NewReadingGoalDialog(
            books = displayedBooks,
            onDismiss = { viewModel.showNewGoalDialog.value = false },
            onConfirm = { title, goalType, bookId, bookTitle, targetPages, targetDays ->
                viewModel.createReadingGoal(
                    title = title,
                    goalType = goalType,
                    bookId = bookId,
                    bookTitle = bookTitle,
                    targetPages = targetPages,
                    targetDays = targetDays
                )
            }
        )
    }

    // CBR Format Notice Dialog
    if (showCbrInfo) {
        AlertDialog(
            onDismissRequest = { viewModel.showCbrInfoDialog.value = false },
            title = { Text("Arquivo CBR Importado") },
            text = {
                Text(
                    "O arquivo foi adicionado à sua biblioteca. O formato CBR utiliza compressão RAR proprietária. Para máxima fluidez sem engasgos, recomendamos converter quadrinhos RAR para CBZ (ZIP padrão)."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.showCbrInfoDialog.value = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
