package com.aliplankstu.randomanime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Calendar

private val Ink = Color(0xFF24233A)
private val Violet = Color(0xFF7664D8)
private val Soft = Color(0xFFF5F3FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RandomAnimeApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomAnimeApp() {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var year by remember { mutableIntStateOf(currentYear) }
    var month by remember { mutableIntStateOf(0) } // 0 = 全年
    var completed by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<Anime?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Violet,
            onPrimary = Color.White,
            background = Soft,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        Scaffold(containerColor = Soft) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Violet, shape = RoundedCornerShape(15.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White,
                                modifier = Modifier.padding(12.dp).size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("随机番名", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("今天看点什么？", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
                item {
                    Text("选择番剧状态", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = completed, onClick = { completed = true; result = null },
                            label = { Text("已完结") })
                        FilterChip(selected = !completed, onClick = { completed = false; result = null },
                            label = { Text("未完结") })
                    }
                }
                item {
                    Text("选择年份", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (year > 2000) { year--; result = null } }) { Text("－") }
                        Spacer(Modifier.width(14.dp))
                        Text("${year} 年", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(14.dp))
                        OutlinedButton(onClick = { if (year < currentYear) { year++; result = null } }) { Text("＋") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { year = currentYear; result = null }) { Text("今年") }
                    }
                }
                item {
                    Text("选择月份", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    val labels = listOf("全年") + (1..12).map { "${it}月" }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                for (col in 0..3) {
                                    val index = row * 4 + col
                                    if (index < labels.size) {
                                        FilterChip(
                                            selected = month == index,
                                            onClick = { month = index; result = null },
                                            label = { Text(labels[index], fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                result = null
                                try {
                                    result = AniListRepository.randomAnime(year, month, completed)
                                    if (result == null) error = "没有找到符合条件的番剧，试试其他月份吧。"
                                } catch (e: Exception) {
                                    error = "获取番剧失败：${e.message ?: "请检查网络后重试"}"
                                } finally { loading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !loading
                    ) {
                        if (loading) CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                        ) else Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "正在寻找番剧…" else "随机抽取一部", fontSize = 16.sp)
                    }
                }
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                error?.let { message ->
                    item {
                        Surface(color = Color(0xFFFFEEEE), shape = RoundedCornerShape(12.dp)) {
                            Text(message, color = Color(0xFF9B3B3B), modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                result?.let { anime ->
                    item {
                        Text("抽取结果", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                AsyncImage(
                                    model = anime.cover,
                                    contentDescription = anime.title,
                                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(anime.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Spacer(Modifier.height(8.dp))
                                Text("${anime.year} 年 · ${anime.month} 月 · ${if (anime.completed) "已完结" else "未完结"}",
                                    color = Violet, fontSize = 13.sp)
                                anime.nativeTitle?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(5.dp))
                                    Text(it, color = Color.Gray, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("数据来源：AniList", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    try { result = AniListRepository.randomAnime(year, month, completed) }
                                    catch (e: Exception) { error = "重新抽取失败，请稍后再试。" }
                                    finally { loading = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("再随机一次")
                        }
                    }
                }
                item {
                    Text("番剧资料按开播月份归类。全年模式会从所选年份的所有月份中随机抽取。",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

data class Anime(
    val title: String,
    val nativeTitle: String?,
    val cover: String?,
    val year: Int,
    val month: Int,
    val completed: Boolean
)
