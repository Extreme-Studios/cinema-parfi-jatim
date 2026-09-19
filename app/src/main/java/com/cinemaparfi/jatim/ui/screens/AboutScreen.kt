package com.cinemaparfi.jatim.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinemaparfi.jatim.BuildConfig
import com.cinemaparfi.jatim.R

private val AboutBlack = Color(0xFF030607)
private val AboutGold = Color(0xFFE7B946)
private val AboutPaper = Color(0xFFF5F2EB)
private val AboutMuted = Color(0xFFB5B6B8)
private val AboutCard = Color(0xFF11161A)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val values = listOf(
        AboutValue("Kreativitas", "Mendorong ide-ide baru dari semua kalangan.", Icons.Default.AutoAwesome),
        AboutValue("Kolaborasi", "Bersama komunitas untuk ekosistem film yang lebih kuat.", Icons.Default.Groups),
        AboutValue("Budaya Lokal", "Mengangkat cerita dan nilai-nilai Jawa Timur.", Icons.Default.Movie),
        AboutValue("Dampak Positif", "Film sebagai sarana edukasi, inspirasi, dan perubahan.", Icons.Default.Favorite)
    )
    LazyColumn(
        modifier = modifier.fillMaxSize().background(AboutBlack),
        contentPadding = PaddingValues(bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { AboutHero() }
        item {
            Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tentang", color = AboutPaper, fontSize = 39.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(10.dp))
                    Text("Kami", color = AboutGold, fontSize = 39.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    "Cinema PARFI Jatim adalah ruang kurasi film indie, short movie, dan cerita kebudayaan. Kami memberi panggung bagi kreator lokal agar karya dan budaya Jawa Timur dapat dinikmati lebih banyak orang.",
                    color = AboutPaper.copy(alpha = 0.87f),
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                )
            }
        }
        item { AboutFocusRow() }
        item {
            Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nilai", color = AboutPaper, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text("Kami", color = AboutGold, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Prinsip yang selalu kami pegang dalam setiap langkah.", color = AboutMuted, fontSize = 16.sp)
            }
        }
        item {
            LazyColumn(
                modifier = Modifier.height(202.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(values.chunked(2)) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { value -> AboutValueCard(value = value, modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        item { AboutCallToAction() }
        item { AboutFooter() }
    }
}

@Composable
private fun AboutHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(292.dp).background(
            Brush.linearGradient(listOf(Color(0xFF05080C), Color(0xFF172232), AboutBlack))
        )
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 28.dp)) {
            Image(painterResource(R.drawable.logo_parfi_asli), "Logo resmi PARFI", Modifier.size(104.dp))
            Spacer(Modifier.height(8.dp))
            Text("Cinema", color = AboutGold, fontSize = 33.sp, fontWeight = FontWeight.Light)
            Text("PARFI Jatim", color = AboutPaper, fontSize = 37.sp, fontWeight = FontWeight.ExtraBold)
            Text("FILM  •  SENI  •  KITA", color = AboutMuted, fontSize = 11.sp, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(20.dp))
            Text("RUANG UNTUK CERITA\nYANG LEBIH BAIK", color = AboutPaper.copy(alpha = 0.75f), fontSize = 13.sp, letterSpacing = 3.sp, lineHeight = 25.sp)
        }
        Text("FILM\nSENI\nKITA", color = AboutGold.copy(alpha = 0.78f), fontSize = 15.sp, letterSpacing = 4.sp, lineHeight = 34.sp, modifier = Modifier.align(Alignment.TopEnd).padding(38.dp))
    }
}

@Composable
private fun AboutFocusRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AboutFocusCard("Karya\nKreator Lokal", Icons.Default.Movie, Modifier.weight(1f))
        AboutFocusCard("Komunitas\nBertumbuh", Icons.Default.Groups, Modifier.weight(1f))
        AboutFocusCard("Budaya\nLebih Dikenal", Icons.Default.Star, Modifier.weight(1f))
    }
}

@Composable
private fun AboutFocusCard(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(
        modifier = modifier.height(142.dp).border(1.dp, AboutPaper.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .background(AboutCard, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = AboutGold, modifier = Modifier.size(28.dp))
        Text(text, color = AboutPaper, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun AboutValueCard(value: AboutValue, modifier: Modifier) {
    Column(
        modifier = modifier.height(96.dp).border(1.dp, AboutPaper.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .background(AboutCard, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(value.icon, null, tint = AboutGold, modifier = Modifier.size(22.dp))
        Text(value.title, color = AboutPaper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(value.body, color = AboutMuted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
    }
}

@Composable
private fun AboutCallToAction() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(205.dp).background(
            Brush.horizontalGradient(listOf(Color(0xFF1C2730), Color(0xFF3C2A18), Color(0xFF15191B))), RoundedCornerShape(16.dp)
        ).padding(22.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bersama, Kita", color = AboutPaper, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Majukan Film Jawa Timur", color = AboutGold, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Mari dukung karya lokal, temukan talenta baru, dan jadilah bagian dari perjalanan ini.", color = AboutPaper.copy(alpha = 0.86f), fontSize = 14.sp, maxLines = 2)
        }
    }
}

@Composable
private fun AboutFooter() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_parfi_asli), "PARFI", Modifier.size(55.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cinema PARFI Jatim", color = AboutPaper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("FILM  •  SENI  •  KITA", color = AboutGold, fontSize = 9.sp, letterSpacing = 1.4.sp)
        }
        Text("Versi ${BuildConfig.VERSION_NAME}\n© 2026 PARFI Jatim", color = AboutMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private data class AboutValue(
    val title: String,
    val body: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
