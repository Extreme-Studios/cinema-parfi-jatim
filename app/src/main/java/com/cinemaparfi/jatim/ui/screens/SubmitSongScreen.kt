package com.cinemaparfi.jatim.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cinemaparfi.jatim.data.model.SongRegistrationPayload
import com.cinemaparfi.jatim.data.repository.SongRegistrationRepository
import com.cinemaparfi.jatim.ui.SelectedFilePayload
import com.cinemaparfi.jatim.ui.SelectedImagePayload
import com.cinemaparfi.jatim.ui.readSelectedFile
import com.cinemaparfi.jatim.ui.readSelectedImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitSongScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    repository: SongRegistrationRepository = remember { SongRegistrationRepository() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryOptions = listOf("Action", "Horror", "Adventure", "Dokumenter", "Sejarah", "Edukasi", "Short Video")
    var movieTitle by remember { mutableStateOf("") }
    var releaseInfo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var movieLink by remember { mutableStateOf("") }
    var trailerLink by remember { mutableStateOf("") }
    var posterManualLink by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCoverImage by remember { mutableStateOf<SelectedImagePayload?>(null) }
    var selectedTrailerFile by remember { mutableStateOf<SelectedFilePayload?>(null) }

    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedImage(context, uri)
                .onSuccess {
                    selectedCoverImage = it
                    fieldErrors.remove("coverImage")
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(
                        error.message ?: "Gagal memuat cover artwork."
                    )
                }
        }
    }
    val trailerPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedFile(context, uri)
                .onSuccess {
                    selectedTrailerFile = it
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(
                        error.message ?: "Gagal memuat file trailer."
                    )
                }
        }
    }

    fun validate(): Boolean {
        fieldErrors.clear()
        if (movieTitle.isBlank()) fieldErrors["movieTitle"] = "Judul Film wajib diisi"
        if (movieLink.isBlank()) fieldErrors["movieLink"] = "Link Film wajib diisi"
        return fieldErrors.isEmpty()
    }

    fun clearForm() {
        movieTitle = ""
        releaseInfo = ""
        category = ""
        synopsis = ""
        movieLink = ""
        trailerLink = ""
        posterManualLink = ""
        selectedCoverImage = null
        selectedTrailerFile = null
        fieldErrors.clear()
    }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                item {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Registrasi Film",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_revert),
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
            if (onBack == null) {
                item {
                    Text(
                        text = "Registrasi Film",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item { FormField(movieTitle, { movieTitle = it }, "1. Judul", fieldErrors["movieTitle"]) }
            item { FormField(releaseInfo, { releaseInfo = it }, "2. Release (Opsional)", null) }
            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("3. Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item { FormField(synopsis, { synopsis = it }, "4. Sinopsis", null, minLines = 3) }
            item { FormField(movieLink, { movieLink = it }, "5. Link Film", fieldErrors["movieLink"]) }
            item { FormField(trailerLink, { trailerLink = it }, "6. Link Trailer", null) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih Trailer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { trailerPickerLauncher.launch("video/*") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(if (selectedTrailerFile == null) "Pilih File Trailer" else "Ganti File Trailer")
                    }
                    selectedTrailerFile?.let { trailer ->
                        Text(
                            text = "${trailer.fileName} • ${(trailer.sizeBytes / 1024)} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { FormField(posterManualLink, { posterManualLink = it }, "7. Link Poster Film", null) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih Poster Film",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pilih gambar poster dari galeri. File akan diupload ke folder Cover Google Drive.",
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
                        Text(if (selectedCoverImage == null) "Pilih Cover Artwork" else "Ganti Cover Artwork")
                    }
                    selectedCoverImage?.let { cover ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = cover.previewBitmap.asImageBitmap(),
                                contentDescription = "Preview cover artwork",
                                modifier = Modifier
                                    .size(84.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = cover.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = cover.mimeType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (!validate()) {
                            scope.launch { snackbarHostState.showSnackbar("Form belum lengkap. Cek kembali data wajib.") }
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val payload = SongRegistrationPayload(
                                songTitle = movieTitle.trim(),
                                release = releaseInfo.trim(),
                                category = category.trim(),
                                synopsis = synopsis.trim(),
                                movieLink = movieLink.trim(),
                                trailerLink = trailerLink.trim(),
                                posterLink = posterManualLink.trim(),
                                copyrightAgreement = true,
                                coverImageBase64 = selectedCoverImage?.base64,
                                coverImageFileName = selectedCoverImage?.fileName,
                                coverImageMimeType = selectedCoverImage?.mimeType,
                                trailerFileBase64 = selectedTrailerFile?.base64,
                                trailerFileName = selectedTrailerFile?.fileName,
                                trailerFileMimeType = selectedTrailerFile?.mimeType
                            )

                            val result = repository.submitRegistration(payload)
                            isLoading = false

                            result.onSuccess {
                                snackbarHostState.showSnackbar(
                                    "Registrasi film berhasil dikirim. Tim Cinema PARFI Jatim akan melakukan review terlebih dahulu."
                                )
                                clearForm()
                            }.onFailure { error ->
                                snackbarHostState.showSnackbar(
                                    "Gagal mengirim registrasi: ${error.message ?: "Terjadi kesalahan, coba lagi."}"
                                )
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Kirim Registrasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    minLines: Int = 1
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(
            imeAction = if (minLines == 1) ImeAction.Done else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus(force = true) }
        ),
        supportingText = {
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
