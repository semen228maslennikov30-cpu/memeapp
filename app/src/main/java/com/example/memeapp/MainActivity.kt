package com.example.memeapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val DAILY_LIMIT = 20

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MemeScreen() }
    }
}

private val captions = listOf(
    "КОГДА ПОНЯЛ," to "ЧТО ЗАБЫЛ ВЫКЛЮЧИТЬ ПЛИТУ",
    "ПОНЕДЕЛЬНИК" to "ЭТО ПРОСТО СОСТОЯНИЕ ДУШИ",
    "Я НА РАБОТЕ:" to "ДУМАЮ ТОЛЬКО О ПЕРЕРЫВЕ",
    "КОГДА ХОЧЕШЬ СПАТЬ," to "НО ВСПОМНИЛ ПРО ДЕДЛАЙН"
)

private val randomPrompts = listOf(
    "кот в космосе", "программист пьёт кофе", "панда на скейтборде",
    "динозавр в офисе", "утка в очках"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("meme", Context.MODE_PRIVATE) }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val resetText = remember(today) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        SimpleDateFormat("d MMMM, HH:mm", Locale.forLanguageTag("ru-RU")).format(cal.time)
    }

    var dark by rememberSaveable { mutableStateOf(false) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var topText by rememberSaveable { mutableStateOf("") }
    var bottomText by rememberSaveable { mutableStateOf("") }
    var used by remember {
        mutableIntStateOf(if (prefs.getString("day", null) == today) prefs.getInt("count", 0) else 0)
    }

    fun generate(text: String, top: String = "", bottom: String = "") {
        val clean = text.trim()
        if (clean.isEmpty() || used >= DAILY_LIMIT) return
        used++
        prefs.edit().putString("day", today).putInt("count", used).apply()
        topText = top
        bottomText = bottom
        val encoded = URLEncoder.encode(clean, "UTF-8").replace("+", "%20")
        val seed = (0..999_999).random()
        imageUrl = "https://image.pollinations.ai/prompt/$encoded?width=512&height=512&nologo=true&seed=$seed"
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF0A84FF), background = Color.Black,
            surface = Color(0xFF1C1C1E), onSurface = Color.White,
            onSurfaceVariant = Color(0xFF8E8E93)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF007AFF), background = Color(0xFFF2F2F7),
            surface = Color.White, onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF8E8E93)
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = scheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Мемы", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        Text(if (dark) "☾" else "☀︎", color = scheme.onSurfaceVariant)
                        Switch(checked = dark, onCheckedChange = { dark = it })
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = scheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surface)
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            "Введите текст или нажмите на кубик",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    val memeStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(Color.Black, blurRadius = 6f)
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(topText, style = memeStyle)
                        Text(bottomText, style = memeStyle)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Опишите мем") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { generate(prompt) }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val caption = captions.random()
                            generate(randomPrompts.random(), caption.first, caption.second)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFFF9500)
                        )
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = "Случайный мем", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Осталось: ${DAILY_LIMIT - used} из $DAILY_LIMIT, сброс $resetText",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "от cloude с moto❣️",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
