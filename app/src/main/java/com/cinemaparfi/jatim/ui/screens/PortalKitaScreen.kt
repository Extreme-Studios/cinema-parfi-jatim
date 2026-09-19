package com.cinemaparfi.jatim.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.cinemaparfi.jatim.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.cinemaparfi.jatim.data.local.PortalSessionStore
import com.cinemaparfi.jatim.data.model.PortalPost
import com.cinemaparfi.jatim.data.model.PortalUserSession
import com.cinemaparfi.jatim.data.repository.PortalRepository
import com.cinemaparfi.jatim.ui.SelectedImagePayload
import com.cinemaparfi.jatim.ui.readSelectedImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortalKitaScreen(
    posts: List<PortalPost>,
    categories: List<String>,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onPostPreview: (PortalPost) -> Unit,
    onOpenPosterSubmission: () -> Unit
) {
    val context = LocalContext.current
    val sessionStore = remember { PortalSessionStore(context) }
    val repository = remember { PortalRepository() }
    val session by sessionStore.sessionFlow.collectAsState(initial = null)
    val canPostPoster = true
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var reportPost by remember { mutableStateOf<PortalPost?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var deletePost by remember { mutableStateOf<PortalPost?>(null) }
    var boardNotice by remember { mutableStateOf<UiNotice?>(null) }

    var selectedCategory by remember { mutableStateOf("All") }
    val filteredPosts = remember(posts, selectedCategory) {
        posts.filter { selectedCategory == "All" || it.category == selectedCategory }
    }
    val featuredPost = remember(filteredPosts) { filteredPosts.firstOrNull() }

    androidx.compose.runtime.LaunchedEffect(session?.userId, session?.status, canPostPoster) {
        Log.d("PortalKita", "hubSession status=${session?.status ?: "null"}")
        Log.d("PortalKita", "canPostPoster=$canPostPoster")
        Log.d("PortalKita", "Pasang Poster visible=$canPostPoster")
    }

    androidx.compose.runtime.LaunchedEffect(session?.username, session?.password) {
        if (session == null) return@LaunchedEffect
        val result = repository.checkHubStatus(session?.username.orEmpty(), session?.password.orEmpty())
        result.onSuccess { account ->
            when {
                account.status.equals("ACTIVE", ignoreCase = true) -> {
                    val merged = if (account.userId.isBlank() && session?.userId?.isNotBlank() == true) {
                        account.copy(userId = session?.userId.orEmpty())
                    } else account
                    sessionStore.saveSession(merged)
                    Log.d("PortalKita", "Recovered ACTIVE hub session from status check")
                }
                else -> sessionStore.clearSession()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030607)),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            PosterBoardHero(onOpenPosterSubmission = onOpenPosterSubmission)
        }
        item {
            PosterBoardCategoryRail(categories = categories, selected = selectedCategory, onSelected = { selectedCategory = it })
        }
        boardNotice?.let { notice ->
            item {
                NoticeCard(
                    message = notice.message,
                    type = notice.type,
                    onDismiss = { boardNotice = null },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        featuredPost?.let { post ->
            item {
                FeaturedPosterCard(post = post, onClick = { onPostPreview(post) })
            }
        }

        if (isLoading) {
            item {
                Text(
                    text = "Memuat poster...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (errorMessage != null && filteredPosts.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else if (filteredPosts.isEmpty()) {
            item {
                Text(
                    text = "Belum ada poster di kategori ini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item { PosterBoardSectionTitle("Rilis Baru") }
            items(filteredPosts, key = { it.id }) { post ->
                PortalPostCard(
                    post = post,
                    onPrimaryClick = {
                        if (post.targetUrl.isNotBlank()) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(post.targetUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            onPostPreview(post)
                        }
                    },
                    onPreviewClick = { onPostPreview(post) },
                    onReportClick = {
                        reportPost = post
                        reportReason = ""
                    },
                    onDeleteClick = if (session != null) {
                        { deletePost = post }
                    } else null
                )
            }
        }
    }

    reportPost?.let { post ->
        AlertDialog(
            onDismissRequest = { reportPost = null },
            title = { Text("Laporkan Poster") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tulis alasan laporan untuk poster ini.")
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.trim().isBlank()) return@TextButton
                        scope.launch {
                            val result = repository.reportPoster(
                                post = post,
                                reason = reportReason.trim(),
                                reporterWhatsapp = session?.username.orEmpty()
                            )
                            result.onSuccess {
                                boardNotice = UiNotice("Laporan terkirim. Tim akan meninjau konten ini.", UiNoticeType.SUCCESS)
                                reportPost = null
                            }.onFailure { error ->
                                boardNotice = UiNotice(error.message ?: "Laporan gagal.", UiNoticeType.ERROR)
                            }
                        }
                    }
                ) { Text("Kirim") }
            },
            dismissButton = {
                TextButton(onClick = { reportPost = null }) { Text("Batal") }
            }
        )
    }

    deletePost?.let { post ->
        AlertDialog(
            onDismissRequest = { deletePost = null },
            title = { Text("Hapus Poster") },
            text = { Text("Poster ini akan dihapus dari Google Sheet dan tidak tampil lagi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val admin = session ?: return@TextButton
                        scope.launch {
                            val result = repository.deletePoster(admin, post.id)
                            result.onSuccess {
                                boardNotice = UiNotice("Poster berhasil dihapus.", UiNoticeType.SUCCESS)
                                deletePost = null
                                onRefresh()
                            }.onFailure { error ->
                                boardNotice = UiNotice(error.message ?: "Gagal menghapus poster.", UiNoticeType.ERROR)
                            }
                        }
                    }
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deletePost = null }) { Text("Batal") }
            }
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
private fun PosterBoardHero(onOpenPosterSubmission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF07111C), Color(0xFF172A39), Color(0xFF030607))))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_parfi_asli),
                    contentDescription = "PARFI Jawa Timur",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Cinema", color = Color(0xFFE7B946), style = MaterialTheme.typography.titleLarge)
                    Text("PARFI Jatim", color = Color(0xFFF5F2EB), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("FILM  •  SENI  •  KITA", color = Color(0xFFB5B6B8), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Poster", color = Color(0xFFF5F2EB), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(10.dp))
                Text("Board", color = Color(0xFFE7B946), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Ruang poster film indie, short movie,\npemutaran, event, dan budaya lokal.",
                color = Color(0xFFD5D7D8),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onOpenPosterSubmission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7B946), contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) {
                Text("+ Pasang Poster", fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "KARYA\nLOKAL\nINSPIRASI\nUNTUK\nINDONESIA",
            color = Color(0xFFD5D7D8).copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.5.sp,
            lineHeight = 22.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(28.dp)
        )
    }
}

@Composable
private fun PosterBoardCategoryRail(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it }) { category ->
            val active = category == selected
            Surface(
                modifier = Modifier.clickable { onSelected(category) },
                color = if (active) Color(0xFFE7B946) else Color(0xFF101B26),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
                    color = if (active) Color.Black else Color(0xFFF5F2EB),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeaturedPosterCard(post: PortalPost, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF36516D), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        RemotePoster(imageUrl = post.posterUrl, title = post.title, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEF030607), Color(0xA6030607), Color.Transparent))))
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(24.dp)
                .fillMaxWidth(0.70f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("POSTER UNGGULAN", color = Color(0xFFE7B946), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp)
            Text(post.title, color = Color(0xFFF5F2EB), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(post.shortCaption.ifBlank { "Karya lokal untuk Indonesia yang lebih baik." }, color = Color(0xFFD5D7D8), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7B946), contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) { Text("Lihat Detail", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PosterBoardSectionTitle(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color(0xFFF5F2EB), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(34.dp).height(3.dp).background(Color(0xFFE7B946), RoundedCornerShape(3.dp)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortalPosterSubmissionScreen(
    modifier: Modifier = Modifier,
    repository: PortalRepository = remember { PortalRepository() },
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionStore = remember { PortalSessionStore(context) }
    val session by sessionStore.sessionFlow.collectAsState(initial = null)
    var resolvedSession by remember { mutableStateOf<PortalUserSession?>(null) }
    androidx.compose.runtime.LaunchedEffect(session?.userId, session?.status) {
        if (session?.status.equals("ACTIVE", ignoreCase = true)) {
            resolvedSession = session
        } else if (session == null) {
            // keep the last active session latched while submit flow is open
        } else {
            resolvedSession = null
        }
    }
    val canPostPoster = resolvedSession?.status.equals("ACTIVE", ignoreCase = true)
    val categories = remember { repository.getSubmissionCategories() }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedPosterImage by remember { mutableStateOf<SelectedImagePayload?>(null) }
    var agreementChecked by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitNotice by remember { mutableStateOf<UiNotice?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedImage(context, uri)
                .onSuccess {
                    selectedPosterImage = it
                    fieldErrors.remove("posterImage")
                }
                .onFailure { error ->
                    submitNotice = UiNotice(error.message ?: "Gagal memuat poster.", UiNoticeType.ERROR)
                }
        }
    }

    fun validate(): Boolean {
        fieldErrors.clear()
        if (selectedCategory.isBlank()) fieldErrors["category"] = "Kategori poster wajib dipilih"
        if (selectedPosterImage == null) fieldErrors["posterImage"] = "Poster wajib dipilih"
        if (!agreementChecked) fieldErrors["agreement"] = "Persetujuan wajib dicentang"
        return fieldErrors.isEmpty()
    }

    fun clearForm() {
        selectedCategory = ""
        selectedPosterImage = null
        agreementChecked = false
        fieldErrors.clear()
    }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Kembali")
                }
            }
            item {
                Text(
                    text = "Pasang Poster",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            submitNotice?.let { notice ->
                item {
                    NoticeCard(
                        message = notice.message,
                        type = notice.type,
                        onDismiss = { submitNotice = null }
                    )
                }
            }
            item {
                if (resolvedSession != null) {
                    LoggedInPortalHeader(session = resolvedSession ?: return@item)
                } else {
                    Text(
                        text = "Kirim sebagai tamu. Poster akan masuk ke antrian review admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    fieldErrors.remove("category")
                                },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    fieldErrors["category"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Poster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pilih poster JPG atau PNG langsung dari galeri HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(if (selectedPosterImage == null) "Pilih Poster" else "Ganti Poster")
                    }
                    selectedPosterImage?.let { poster ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                bitmap = poster.previewBitmap.asImageBitmap(),
                                contentDescription = "Preview poster",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = poster.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    fieldErrors["posterImage"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Poster Board menerima poster film indie, short movie, pemutaran, dan kegiatan kebudayaan. Konten di luar tema akan ditolak admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    )
                }
            }
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = agreementChecked,
                            onCheckedChange = {
                                agreementChecked = it
                                fieldErrors.remove("agreement")
                            }
                        )
                        Text(
                            "Saya menyatakan poster ini berkaitan dengan film indie atau kebudayaan dan saya memiliki hak/izin untuk mengirimkannya ke Poster Board.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    fieldErrors["agreement"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item {
                Button(
                    onClick = {
                        if (!validate()) {
                            submitNotice = UiNotice("Form poster belum lengkap.", UiNoticeType.WARNING)
                            return@Button
                        }
                        isSubmitting = true
                        scope.launch {
                            val result = repository.submitPoster(
                                session = resolvedSession,
                                category = selectedCategory,
                                posterImageBase64 = requireNotNull(selectedPosterImage).base64,
                                posterImageFileName = requireNotNull(selectedPosterImage).fileName,
                                posterImageMimeType = requireNotNull(selectedPosterImage).mimeType
                            )
                            isSubmitting = false
                            result.onSuccess {
                                submitNotice = UiNotice("Poster berhasil dipasang di Poster Board.", UiNoticeType.SUCCESS)
                                clearForm()
                                onSubmitted()
                            }.onFailure { error ->
                                submitNotice = UiNotice(error.message ?: "Gagal mengirim poster.", UiNoticeType.ERROR)
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isSubmitting) {
                        Text("Mengirim...")
                    } else {
                        Text("Kirim Poster", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggedInPortalHeader(
    session: PortalUserSession
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Logged in as: ${session.bandName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Username: ${session.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PortalPostDetailScreen(
    post: PortalPost,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isFullscreenPreview by remember { mutableStateOf(false) }
    if (isFullscreenPreview) {
        FullscreenPosterViewer(
            imageUrl = post.posterUrl,
            title = post.title,
            onExit = { isFullscreenPreview = false }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Text("Kembali")
            }
        }
        item {
            RemotePoster(
                imageUrl = post.posterUrl,
                title = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clickable { isFullscreenPreview = true }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = post.displayDate(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = {},
                    label = { Text(post.category) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (post.shortCaption.isNotBlank()) {
                    Text(
                        text = post.shortCaption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (post.eventDate.isNotBlank()) {
                    Text(
                        text = post.eventDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (post.targetUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(post.targetUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) {
                        Text("Buka Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalPostCard(
    post: PortalPost,
    onPrimaryClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onPrimaryClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10171E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, Color(0x334C637B), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RemotePoster(
                imageUrl = post.posterUrl,
                title = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!post.submitterName.equals(post.title, ignoreCase = true)) {
                    Text(
                        text = post.submitterName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(post.category) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = null
                    )
                    if (post.eventDate.isNotBlank()) {
                        Text(
                            text = post.eventDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (post.shortCaption.isNotBlank()) {
                    Text(
                        text = post.shortCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onPreviewClick) {
                        Text("Preview")
                    }
                    TextButton(onClick = onReportClick) {
                        Text("Laporkan")
                    }
                    if (onDeleteClick != null) {
                        TextButton(onClick = onDeleteClick) {
                            Text("Hapus")
                        }
                    }
                    if (post.targetUrl.isNotBlank()) {
                        TextButton(onClick = onPrimaryClick) {
                            Text("Buka Link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemotePoster(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "$title poster",
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        onLoading = { },
        onError = { }
    )
}

@Composable
private fun FullscreenPosterViewer(
    imageUrl: String,
    title: String,
    onExit: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        if (nextScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(scale) {
                detectTapGestures(
                    onTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            onExit()
                        }
                    },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.4f
                        }
                    }
                )
            }
            .transformable(transformableState),
        contentAlignment = Alignment.Center
    ) {
        RemotePoster(
            imageUrl = imageUrl,
            title = title,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

private fun PortalPost.displayDate(): String {
    val rawDate = eventDate.ifBlank { createdAt }.trim()
    if (rawDate.isBlank()) return "Tanggal posting belum tersedia"
    return runCatching {
        val parsed = OffsetDateTime.parse(rawDate)
        "Diposting ${parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))}"
    }.getOrElse {
        "Diposting $rawDate"
    }
}

private enum class UiNoticeType { SUCCESS, WARNING, ERROR }

private data class UiNotice(
    val message: String,
    val type: UiNoticeType
)

@Composable
private fun NoticeCard(
    message: String,
    type: UiNoticeType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, border) = when (type) {
        UiNoticeType.SUCCESS -> Triple(Color(0x1A2E7D32), Color(0xFFB9F6CA), Color(0x6632C45A))
        UiNoticeType.WARNING -> Triple(Color(0x1A7A5A00), Color(0xFFFFE082), Color(0x66FFC107))
        UiNoticeType.ERROR -> Triple(Color(0x1A8E2430), Color(0xFFFFCDD2), Color(0x66EF5350))
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(14.dp)),
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = fg)
            }
        }
    }
}
