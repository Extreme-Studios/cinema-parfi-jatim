package com.cinemaparfi.jatim

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import com.cinemaparfi.jatim.R
import com.cinemaparfi.jatim.BuildConfig
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cinemaparfi.jatim.navigation.CinemaParfiJatimApp
import com.cinemaparfi.jatim.ui.theme.CinemaParfiJatimTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}
        setContent {
            CinemaParfiJatimTheme {
                var splashFinished by remember { mutableStateOf(false) }
                var continueToApp by remember { mutableStateOf(false) }
                if (!splashFinished) {
                    SplashScreen(onFinish = { splashFinished = true })
                } else if (!continueToApp) {
                    LaunchedEffect(Unit) {
                        continueToApp = true
                    }
                } else {
                    CinemaParfiJatimApp()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500)
        onFinish()
    }
    val versionName = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07152D)),
        contentAlignment = Alignment.Center
    ) {
        val splashPainter = painterResource(id = R.drawable.splash_cinema_parfi)
        Image(
            painter = splashPainter,
            contentDescription = "Cinema PARFI Jatim",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFFE7B946),
                strokeWidth = 2.dp
            )

            Text(
                text = "Memuat aplikasi...",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
            )

            Text(
                text = versionName,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )

            Text(
                text = "Powered By EXTREME STUDIOS",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
