package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.reader.ComicReaderEngine
import com.example.reader.EpubChapter
import com.example.reader.EpubReaderEngine
import com.example.reader.EpubSearchResult
import com.example.reader.PdfReaderEngine
import com.example.reader.TTSManager
import com.example.ui.components.AddHighlightDialog
import com.example.ui.components.BookmarksAndNotesSheet
import com.example.ui.theme.DarkEspressoBg
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaTextColor
import com.example.ui.viewmodel.BookViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: BookEntity,
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Incognito state
    val isIncognito by viewModel.isIncognitoMode.collectAsState()

    // Current page (1-based index)
    var currentPage by remember { mutableIntStateOf(book.currentPage.coerceAtLeast(1)) }
    var totalPages by remember { mutableIntStateOf(book.totalPages.coerceAtLeast(1)) }

    // Session tracking start time
    val sessionStartMs = remember { System.currentTimeMillis() }
    val initialPage = remember { book.currentPage }

    // Reading Mode for Comics: "LTR", "RTL", "VERTICAL_PAGED", "WEBTOON"
    var comicMode by remember { mutableStateOf(book.comicReadingMode) }
    var cropBorders by remember { mutableStateOf(book.cropBorders) }

    // Full screen controls visibility
    var areControlsVisible by remember { mutableStateOf(true) }

    // EPUB Typography & Themes
    var themePreset by remember { mutableStateOf("dark") } // "dark", "sepia", "light", "oled"
    var selectedFontFamily by remember { mutableStateOf("sans") } // "serif", "sans", "mono", "cursive"
    var epubFontSize by remember { mutableFloatStateOf(17f) }
    var epubLineHeightMultiplier by remember { mutableFloatStateOf(1.5f) }
    var epubHorizontalMarginDp by remember { mutableIntStateOf(24) }
    var isTextJustified by remember { mutableStateOf(true) }

    // Brightness overlay & Blue light filter
    var screenBrightness by remember { mutableFloatStateOf(1.0f) }
    var blueLightFilterLevel by remember { mutableFloatStateOf(0.0f) } // 0.0 .. 0.5

    // Modal dialogs
    var showTypographySheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var selectedTextForHighlight by remember { mutableStateOf("") }
    var showComicModeMenu by remember { mutableStateOf(false) }

    // Engines
    val pdfEngine = remember(book.filePath) { if (book.format == "PDF") PdfReaderEngine(book.filePath) else null }
    val comicEngine = remember(book.filePath) { if (book.format == "CBZ") ComicReaderEngine(book.filePath) else null }
    val epubEngine = remember(book.filePath) { if (book.format == "EPUB") EpubReaderEngine(book.filePath) else null }
    val ttsManager = remember(book.id) { if (book.format == "EPUB") TTSManager(context) else null }

    // TTS state
    var showTtsBar by remember { mutableStateOf(false) }
    val isTtsPlaying = ttsManager?.isPlaying?.collectAsState()?.value ?: false
    val ttsParagraphIdx = ttsManager?.currentParagraphIndex?.collectAsState()?.value ?: 0

    // Engine Ready
    var isEngineReady by remember { mutableStateOf(false) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentEpubChapter by remember { mutableStateOf<EpubChapter?>(null) }

    // Bookmarks and Annotations observation
    val bookmarks by viewModel.getBookmarks(book.id).collectAsState(initial = emptyList())
    val annotations by viewModel.getAnnotations(book.id).collectAsState(initial = emptyList())
    val isCurrentPageBookmarked = bookmarks.any { it.pageOrChapter == currentPage }

    // Double-tap and pinch-to-zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    // Auto-save progress
    LaunchedEffect(currentPage, totalPages) {
        if (isEngineReady) {
            viewModel.updateReadingProgress(book.id, currentPage, totalPages)
        }
    }

    // Initialize reader engine
    LaunchedEffect(book.id) {
        when (book.format) {
            "PDF" -> {
                val opened = pdfEngine?.open() ?: false
                if (opened && pdfEngine != null) {
                    totalPages = pdfEngine.pageCount.coerceAtLeast(1)
                    isEngineReady = true
                }
            }
            "CBZ" -> {
                val opened = comicEngine?.open() ?: false
                if (opened && comicEngine != null) {
                    totalPages = comicEngine.pageCount.coerceAtLeast(1)
                    isEngineReady = true
                }
            }
            "EPUB" -> {
                val opened = epubEngine?.open() ?: false
                if (opened && epubEngine != null) {
                    totalPages = epubEngine.chapterCount.coerceAtLeast(1)
                    isEngineReady = true
                }
            }
            else -> isEngineReady = true
        }
    }

    // Load page content whenever page, engine, or crop setting changes
    LaunchedEffect(currentPage, isEngineReady, cropBorders, comicMode) {
        if (!isEngineReady) return@LaunchedEffect
        scale = 1f
        offset = Offset.Zero

        when (book.format) {
            "PDF" -> {
                val p0 = (currentPage - 1).coerceIn(0, totalPages - 1)
                currentBitmap = pdfEngine?.renderPage(p0)
            }
            "CBZ" -> {
                val p0 = (currentPage - 1).coerceIn(0, totalPages - 1)
                currentBitmap = comicEngine?.loadPageBitmap(p0, autoCropBorders = cropBorders)
                // Background pre-cache next pages for 0-lag flipping
                scope.launch { comicEngine?.preloadPages(p0, 2) }
            }
            "EPUB" -> {
                val chIdx = (currentPage - 1).coerceIn(0, (epubEngine?.chapterCount ?: 1) - 1)
                currentEpubChapter = epubEngine?.chapters?.getOrNull(chIdx)
            }
        }
    }

    // Clean up session and engines upon exiting reader
    DisposableEffect(Unit) {
        onDispose {
            val elapsedSec = ((System.currentTimeMillis() - sessionStartMs) / 1000).coerceAtLeast(0)
            val pagesRead = (currentPage - initialPage).coerceAtLeast(1)
            viewModel.logReadingSession(book.id, book.title, elapsedSec, pagesRead)

            pdfEngine?.close()
            comicEngine?.close()
            epubEngine?.close()
            ttsManager?.shutdown()
        }
    }

    BackHandler {
        onBack()
    }

    // Colors according to theme preset
    val readerBgColor = when (themePreset) {
        "sepia" -> SepiaBackground
        "light" -> Color(0xFFFAF6F0)
        "oled" -> Color(0xFF000000)
        else -> DarkEspressoBg
    }

    val readerTextColor = when (themePreset) {
        "sepia" -> SepiaTextColor
        "light" -> Color(0xFF261D19)
        "oled" -> Color(0xFFFFFFFF)
        else -> Color(0xFFF7EFE9)
    }

    val readerFontFamily = when (selectedFontFamily) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
    ) {
        // Main Reading Viewport
        if (!isEngineReady) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Carregando ${book.format}...", color = readerTextColor)
                }
            }
        } else {
            when (book.format) {
                "CBZ", "PDF" -> {
                    if (book.format == "CBZ" && comicMode == "WEBTOON") {
                        // Webtoon Continuous Vertical Scroll View
                        val webtoonListState = rememberLazyListState()
                        LazyColumn(
                            state = webtoonListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { areControlsVisible = !areControlsVisible }
                                    )
                                }
                        ) {
                            items((0 until totalPages).toList()) { pageIdx ->
                                var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(pageIdx, cropBorders) {
                                    pageBitmap = comicEngine?.loadPageBitmap(pageIdx, autoCropBorders = cropBorders)
                                }

                                pageBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Página ${pageIdx + 1}",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } ?: Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().height(400.dp)
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    } else {
                        // Paged Viewport (LTR, RTL, Vertical Paged) with double-tap zoom & pinch
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformableState)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            // Smooth double tap zoom toggle
                                            scale = if (scale > 1.2f) 1f else 2.5f
                                            offset = Offset.Zero
                                        },
                                        onTap = { tapOffset ->
                                            val screenWidth = size.width
                                            val leftZone = screenWidth * 0.25f
                                            val rightZone = screenWidth * 0.75f

                                            when {
                                                tapOffset.x < leftZone -> {
                                                    // Left tap: advance if RTL (Mangá), or go previous if LTR
                                                    if (comicMode == "RTL") {
                                                        if (currentPage < totalPages) currentPage++
                                                    } else {
                                                        if (currentPage > 1) currentPage--
                                                    }
                                                }
                                                tapOffset.x > rightZone -> {
                                                    // Right tap: go previous if RTL (Mangá), or advance if LTR
                                                    if (comicMode == "RTL") {
                                                        if (currentPage > 1) currentPage--
                                                    } else {
                                                        if (currentPage < totalPages) currentPage++
                                                    }
                                                }
                                                else -> {
                                                    // Center 50% tap: toggle HUD
                                                    areControlsVisible = !areControlsVisible
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            currentBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Página $currentPage",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Página $currentPage", color = readerTextColor)
                            }
                        }
                    }
                }

                "EPUB" -> {
                    // EPUB Scrollable Chapter View with typography and highlighting
                    val epubScrollState = rememberScrollState()
                    LaunchedEffect(currentPage) { epubScrollState.scrollTo(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(epubScrollState)
                            .padding(
                                horizontal = epubHorizontalMarginDp.dp,
                                vertical = if (areControlsVisible) 80.dp else 40.dp
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { areControlsVisible = !areControlsVisible })
                            }
                    ) {
                        Text(
                            text = currentEpubChapter?.title ?: "Capítulo $currentPage",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = readerFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = readerTextColor,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                currentEpubChapter?.paragraphs?.forEachIndexed { idx, paragraph ->
                                    val isHighlightedByTts = isTtsPlaying && ttsParagraphIdx == idx
                                    Text(
                                        text = paragraph,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = epubFontSize.sp,
                                            lineHeight = (epubFontSize * epubLineHeightMultiplier).sp,
                                            textAlign = if (isTextJustified) TextAlign.Justify else TextAlign.Start
                                        ),
                                        fontFamily = readerFontFamily,
                                        color = if (isHighlightedByTts) MaterialTheme.colorScheme.primary else readerTextColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isHighlightedByTts) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Chapter end buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currentPage > 1) {
                                TextButton(onClick = { currentPage-- }) {
                                    Text("← Capítulo Anterior")
                                }
                            } else Spacer(modifier = Modifier.width(1.dp))

                            if (currentPage < totalPages) {
                                TextButton(onClick = { currentPage++ }) {
                                    Text("Próximo Capítulo →")
                                }
                            }
                        }
                    }
                }

                "CBR" -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Arquivo CBR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Arquivos CBR usam compressão RAR proprietária. Para ler instantaneamente sem travamentos, recomendamos converter em ZIP padrão (.cbz).",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onBack) { Text("Voltar à Biblioteca") }
                            }
                        }
                    }
                }
            }
        }

        // Blue light filter overlay
        if (blueLightFilterLevel > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE28743).copy(alpha = blueLightFilterLevel))
            )
        }

        // Brightness dimmer overlay (when screenBrightness < 1.0)
        if (screenBrightness < 1.0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1.0f - screenBrightness) * 0.75f))
            )
        }

        // Top App Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            text = if (book.format == "EPUB") "Capítulo $currentPage de $totalPages"
                            else "Página $currentPage de $totalPages • $comicMode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Bookmark Button
                    IconButton(
                        onClick = {
                            if (isCurrentPageBookmarked) {
                                val bm = bookmarks.find { it.pageOrChapter == currentPage }
                                if (bm != null) viewModel.removeBookmark(bm)
                            } else {
                                viewModel.addBookmark(book.id, currentPage, "Página $currentPage", null)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Marcador",
                            tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // Bookmarks & Notes Panel button
                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Marcadores e Notas", tint = Color.White)
                    }

                    // EPUB In-book Search
                    if (book.format == "EPUB") {
                        IconButton(onClick = { showSearchDialog = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar no Livro", tint = Color.White)
                        }

                        // Text-to-Speech Toggle
                        IconButton(onClick = { showTtsBar = !showTtsBar }) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Leitura em Voz Alta",
                                tint = if (showTtsBar) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }

                    // Comic Reading Mode dropdown (for CBZ)
                    if (book.format == "CBZ") {
                        Box {
                            IconButton(onClick = { showComicModeMenu = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Modo de Leitura", tint = Color.White)
                            }

                            DropdownMenu(expanded = showComicModeMenu, onDismissRequest = { showComicModeMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Página Única (Esq → Dir)") },
                                    onClick = {
                                        comicMode = "LTR"
                                        viewModel.setComicReadingMode(book.id, "LTR")
                                        showComicModeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mangá (Dir → Esq)") },
                                    onClick = {
                                        comicMode = "RTL"
                                        viewModel.setComicReadingMode(book.id, "RTL")
                                        showComicModeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Webtoon (Rolagem Contínua)") },
                                    onClick = {
                                        comicMode = "WEBTOON"
                                        viewModel.setComicReadingMode(book.id, "WEBTOON")
                                        showComicModeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (cropBorders) "Desativar Corte de Bordas" else "Cortar Bordas Brancas") },
                                    leadingIcon = { Icon(Icons.Default.Crop, null) },
                                    onClick = {
                                        cropBorders = !cropBorders
                                        viewModel.setCropBorders(book.id, cropBorders)
                                        showComicModeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Bar & Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.84f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Page Scrub Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = if (currentPage > 1) Color.White else Color.Gray)
                        }

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.toInt().coerceIn(1, totalPages) },
                            valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f).testTag("page_scrubber_slider")
                        )

                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = if (currentPage < totalPages) Color.White else Color.Gray)
                        }
                    }

                    // Reading Controls: Themes, Brightness, Typography Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 4 Theme Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReaderThemeChip("Claro", Color(0xFFFAF6F0), themePreset == "light") { themePreset = "light" }
                            ReaderThemeChip("Sépia", SepiaBackground, themePreset == "sepia") { themePreset = "sepia" }
                            ReaderThemeChip("Escuro", DarkEspressoBg, themePreset == "dark") { themePreset = "dark" }
                            ReaderThemeChip("AMOLED", Color(0xFF000000), themePreset == "oled") { themePreset = "oled" }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Typography Button (for EPUB)
                            if (book.format == "EPUB") {
                                IconButton(onClick = { showTypographySheet = true }) {
                                    Icon(Icons.Default.FormatSize, contentDescription = "Tipografia", tint = Color.White)
                                }
                            }

                            // Incognito Toggle
                            IconButton(onClick = { viewModel.toggleIncognito() }) {
                                Icon(
                                    imageVector = if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Modo Incógnito",
                                    tint = if (isIncognito) Color(0xFFE63946) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Native Text-To-Speech (TTS) Control Bar (Floating)
        if (showTtsBar && ttsManager != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.90f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (areControlsVisible) 130.dp else 24.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = { ttsManager.prevParagraph() }) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            if (isTtsPlaying) {
                                ttsManager.pauseOrResume()
                            } else {
                                val paragraphs = currentEpubChapter?.paragraphs ?: emptyList()
                                ttsManager.startReading(paragraphs, ttsParagraphIdx)
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { ttsManager.nextParagraph() }) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White)
                    }

                    // Speed Selector
                    var showSpeedMenu by remember { mutableStateOf(false) }
                    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
                    Box {
                        TextButton(onClick = { showSpeedMenu = true }) {
                            Text("${currentSpeed}x", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        currentSpeed = spd
                                        ttsManager.setSpeed(spd)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        ttsManager.stop()
                        showTtsBar = false
                    }) {
                        Icon(Icons.Default.Stop, null, tint = Color(0xFFE63946))
                    }
                }
            }
        }

        // EPUB Typography Bottom Sheet
        if (showTypographySheet) {
            ModalBottomSheet(
                onDismissRequest = { showTypographySheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Personalização de Leitura", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Family
                    Text("Fonte do Livro", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FontOptionChip("Sans", selectedFontFamily == "sans") { selectedFontFamily = "sans" }
                        FontOptionChip("Serif", selectedFontFamily == "serif") { selectedFontFamily = "serif" }
                        FontOptionChip("Mono", selectedFontFamily == "mono") { selectedFontFamily = "mono" }
                        FontOptionChip("Cursiva", selectedFontFamily == "cursive") { selectedFontFamily = "cursive" }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Size Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tamanho da Fonte: ${epubFontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = epubFontSize,
                        onValueChange = { epubFontSize = it },
                        valueRange = 12f..32f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Line Height & Margins
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Espaçamento: ${String.format("%.1f", epubLineHeightMultiplier)}x", style = MaterialTheme.typography.bodySmall)
                        Text("Margem: ${epubHorizontalMarginDp}dp", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Slider(
                            value = epubLineHeightMultiplier,
                            onValueChange = { epubLineHeightMultiplier = it },
                            valueRange = 1.2f..2.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = epubHorizontalMarginDp.toFloat(),
                            onValueChange = { epubHorizontalMarginDp = it.toInt() },
                            valueRange = 16f..44f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Blue Light Filter & Brightness
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Filtro de Luz Azul", style = MaterialTheme.typography.bodySmall)
                        Text("${(blueLightFilterLevel * 200).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = blueLightFilterLevel,
                        onValueChange = { blueLightFilterLevel = it },
                        valueRange = 0.0f..0.5f
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Bookmarks & Notes Sheet
        if (showBookmarksSheet) {
            BookmarksAndNotesSheet(
                book = book,
                bookmarks = bookmarks,
                annotations = annotations,
                onDismiss = { showBookmarksSheet = false },
                onNavigateToPage = { target ->
                    currentPage = target
                    showBookmarksSheet = false
                },
                onDeleteBookmark = { bm -> viewModel.removeBookmark(bm) },
                onDeleteAnnotation = { an -> viewModel.deleteAnnotation(an) }
            )
        }

        // In-Book Text Search Dialog
        if (showSearchDialog && epubEngine != null) {
            InBookSearchDialog(
                epubEngine = epubEngine,
                onDismiss = { showSearchDialog = false },
                onSelectResult = { chIdx ->
                    currentPage = chIdx + 1
                    showSearchDialog = false
                }
            )
        }
    }
}

@Composable
fun ReaderThemeChip(
    label: String,
    bg: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (bg == SepiaBackground || bg == Color(0xFFFAF6F0)) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun FontOptionChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun InBookSearchDialog(
    epubEngine: EpubReaderEngine,
    onDismiss: () -> Unit,
    onSelectResult: (chapterIndex: Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<EpubSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar no Livro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) {
                            isSearching = true
                            scope.launch {
                                results = epubEngine.searchInBook(it)
                                isSearching = false
                            }
                        } else {
                            results = emptyList()
                        }
                    },
                    label = { Text("Palavra ou frase...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (results.isNotEmpty()) {
                    Text("${results.size} ocorrências encontradas:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results) { res ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onSelectResult(res.chapterIndex) }
                                    .padding(8.dp)
                            ) {
                                Text(res.chapterTitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(res.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    Text("Nenhum resultado encontrado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
