package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalLifeOsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class AcademicPdfPage(val image: Bitmap, val count: Int)

@Composable
fun AcademicReference(reference: String, label: String = "Open source page") {
    val parts = reference.removePrefix("syllabus://").split('/')
    val source = parts.firstOrNull().orEmpty()
    val allowed = setOf("calendar", "mathematics", "electrical", "chemistry", "graphics", "life_skills", "python")
    if (!reference.startsWith("syllabus://") || source !in allowed) { return }
    val start = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    var showSource by rememberSaveable(reference) { mutableStateOf(false) }
    WorkspaceTextButton(onClick = { showSource = true }) {
        Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp)); Text("$label · p$start", fontSize = 12.sp)
    }
    if (showSource) AcademicPdfDialog(source, start) { showSource = false }
}

@Composable
private fun AcademicPdfDialog(source: String, start: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalLifeOsColors.current
    var page by rememberSaveable(source) { mutableIntStateOf(start) }
    var failure by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val rendered by produceState<AcademicPdfPage?>(null, source, page) {
        value = null; failure = null; zoom = 1f; offset = Offset.Zero
        val requestedPage = page
        try {
            value = withContext(Dispatchers.IO) {
                val file = File.createTempFile("academic-", ".pdf", context.cacheDir)
                try {
                    context.assets.open("academic/$source.pdf").use { input -> file.outputStream().use { input.copyTo(it) } }
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            require(requestedPage in 1..renderer.pageCount) { "Source page is outside this document." }
                            renderer.openPage(requestedPage - 1).use { sourcePage ->
                                val width = 1400
                                val height = (sourcePage.height.toFloat() / sourcePage.width * width).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(AndroidColor.WHITE)
                                sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                AcademicPdfPage(bitmap, renderer.pageCount)
                            }
                        }
                    }
                } finally { file.delete() }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (error: Exception) { failure = error.message ?: "Source could not open." }
    }
    val transform = rememberTransformableState { factor, pan, _ -> zoom = (zoom * factor).coerceIn(1f, 4f); offset += pan }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(colors.background).systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Original supplied PDF", color = colors.textPrimary, fontSize = 18.sp)
                    Text("${source.replace('_', ' ')} · page $page", color = colors.textSecondary, fontSize = 12.sp)
                }
                WorkspaceIconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close source") }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().transformable(transform), contentAlignment = Alignment.Center) {
                rendered?.let { result -> Image(result.image.asImageBitmap(), "$source, original page $page",
                    Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = offset.x; translationY = offset.y }) }
                    ?: if (failure == null) CircularProgressIndicator(color = colors.primaryAccent) else Text(failure.orEmpty(), color = colors.textPrimary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                WorkspaceIconButton(enabled = rendered != null && page > 1, onClick = { page-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous source page") }
                WorkspaceTextButton(modifier = Modifier.weight(1f), onClick = { zoom = if (zoom == 1f) 2f else 1f; offset = Offset.Zero }) { Text(if (zoom == 1f) "Zoom in" else "Reset zoom") }
                val pageCount = rendered?.count?.toString().orEmpty().ifEmpty { "…" }
                Text("$page / $pageCount", color = colors.textSecondary, fontSize = 12.sp)
                WorkspaceIconButton(enabled = rendered?.let { page < it.count } == true, onClick = { page++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next source page") }
            }
            Text("Pinch to zoom; drag to pan.", color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}
