package com.sl.parknote.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.locationbutton.compose.LocationButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sl.parknote.R
import com.sl.parknote.domain.ParkingRecord
import com.sl.parknote.location.OneShotLocation
import com.sl.parknote.navigation.ExternalMaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun ParkNoteScreen(model: ParkingViewModel) {
    val record by model.parking.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val working by model.working.collectAsStateWithLifecycle()
    val damaged by model.problem.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var mode by rememberSaveable { mutableStateOf("home") }
    var confirmFinish by remember { mutableStateOf(false) }
    val saved = stringResource(R.string.saved)
    val failed = stringResource(R.string.save_failed)
    val mapMissing = stringResource(R.string.no_map)
    val finished = stringResource(R.string.finished)
    fun abandon() {
        if (working) return
        mode = "home"
        scope.launch { runCatching { model.photos.removeUnreferenced(record?.photos?.toSet() ?: emptySet()) } }
    }
    BackHandler(mode != "home") { abandon() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        Surface(tonalElevation = 2.dp) {
            Text(stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                !ready -> CircularProgressIndicator()
                damaged -> Text(stringResource(R.string.damaged_data), color = MaterialTheme.colorScheme.error)
                mode == "new" || mode == "underground" || mode == "edit" -> {
                    Editor(model = model, initial = if (mode == "edit") record else null,
                        working = working, onCancel = { abandon() },
                        onSaved = { mode = "home"; scope.launch { snackbar.showSnackbar(saved) } },
                        onError = { scope.launch { snackbar.showSnackbar(failed) } })
                }
                record == null -> {
                    Spacer(Modifier.height(36.dp))
                    Text(stringResource(R.string.empty_message), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { mode = "new" }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.remember_place))
                    }
                    OutlinedButton(onClick = { mode = "underground" }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.underground))
                    }
                }
                else -> {
                    val active = record!!
                    ActiveParking(active, model)
                    if (active.hasCoordinates) {
                        Button(onClick = {
                            if (!ExternalMaps.open(context, active.latitude!!, active.longitude!!))
                                scope.launch { snackbar.showSnackbar(mapMissing) }
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.find_car)) }
                    }
                    OutlinedButton(onClick = { mode = "edit" }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.edit))
                    }
                    TextButton(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.finish))
                    }
                }
            }
        }
    }
    if (confirmFinish) AlertDialog(onDismissRequest = { confirmFinish = false },
        title = { Text(stringResource(R.string.finish)) },
        text = { Text(stringResource(R.string.delete_warning)) },
        confirmButton = {
            TextButton(enabled = !working, onClick = {
                model.finish { ok ->
                    if (ok) { confirmFinish = false; scope.launch { snackbar.showSnackbar(finished) } }
                    else scope.launch { snackbar.showSnackbar(failed) }
                }
            }) { Text(stringResource(R.string.delete)) }
        }, dismissButton = {
            TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.cancel)) }
        })
}

@Composable
private fun ActiveParking(record: ParkingRecord, model: ParkingViewModel) {
    Text(stringResource(R.string.current_place), style = MaterialTheme.typography.titleLarge)
    if (record.placeName.isNotEmpty()) Text(record.placeName, style = MaterialTheme.typography.headlineSmall)
    if (record.floor.isNotEmpty()) BigDetail(stringResource(R.string.floor), record.floor)
    if (record.sector.isNotEmpty()) BigDetail(stringResource(R.string.sector), record.sector)
    if (record.spaceNumber.isNotEmpty()) BigDetail(stringResource(R.string.space), record.spaceNumber)
    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.createdAt)),
        style = MaterialTheme.typography.bodyMedium)
    if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodyLarge)
    record.photos.forEach { PhotoPreview(model.photos.path(it), onRemove = null) }
    if (record.hasCoordinates) {
        Text(stringResource(R.string.coords_value, record.latitude!!, record.longitude!!))
        record.locationAccuracyMeters?.let { accuracy ->
            if (accuracy > 30f) Text(
                stringResource(if (accuracy > 50f) R.string.low_accuracy else R.string.approximate),
                color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun BigDetail(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Editor(
    model: ParkingViewModel,
    initial: ParkingRecord?,
    working: Boolean,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.placeName ?: "") }
    var floor by rememberSaveable(initial?.id) { mutableStateOf(initial?.floor ?: "") }
    var sector by rememberSaveable(initial?.id) { mutableStateOf(initial?.sector ?: "") }
    var space by rememberSaveable(initial?.id) { mutableStateOf(initial?.spaceNumber ?: "") }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var imageIds by rememberSaveable(initial?.id) { mutableStateOf(initial?.photos?.joinToString("|") ?: "") }
    val pictures = imageIds.split('|').filter { it.isNotEmpty() }
    var latitude by rememberSaveable(initial?.id) { mutableStateOf(initial?.latitude) }
    var longitude by rememberSaveable(initial?.id) { mutableStateOf(initial?.longitude) }
    var accuracy by rememberSaveable(initial?.id) { mutableStateOf(initial?.locationAccuracyMeters) }
    var capturedAt by rememberSaveable(initial?.id) { mutableStateOf(initial?.locationCapturedAt) }
    var provider by rememberSaveable(initial?.id) { mutableStateOf(initial?.locationProvider) }
    var locating by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<Int?>(null) }
    var importing by remember { mutableStateOf(false) }
    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun importPhoto(uri: Uri, camera: Boolean) {
        if (importing || pictures.size >= 2) return
        importing = true
        scope.launch {
            try {
                val id = model.importPhoto(uri)
                imageIds = (pictures + id).joinToString("|")
            } catch (_: Exception) { onError() }
            finally {
                if (camera) runCatching { context.contentResolver.delete(uri, null, null) }
                importing = false
                captureUri = null
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri, false)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        captureUri?.let { text ->
            val uri = Uri.parse(text)
            if (ok) importPhoto(uri, true)
            else { runCatching { context.contentResolver.delete(uri, null, null) }; captureUri = null }
        }
    }

    Text(stringResource(R.string.parking_details), style = MaterialTheme.typography.titleLarge)
    ParkingField(stringResource(R.string.place_name), name, 120) { name = it }
    ParkingField(stringResource(R.string.floor), floor, 40) { floor = it }
    ParkingField(stringResource(R.string.sector), sector, 40) { sector = it }
    ParkingField(stringResource(R.string.space), space, 40) { space = it }
    ParkingField(stringResource(R.string.note), note, 500, multi = true) { note = it }

    if (pictures.isNotEmpty()) Text(stringResource(R.string.photos), style = MaterialTheme.typography.titleMedium)
    pictures.forEach { id ->
        PhotoPreview(model.photos.path(id)) { imageIds = pictures.filterNot { it == id }.joinToString("|") }
    }
    if (pictures.size < 2) {
        OutlinedButton(enabled = !importing && !working, onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(stringResource(R.string.choose_photo)) }
        OutlinedButton(enabled = !importing && !working, onClick = {
            try {
                val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File.createTempFile("photo-", ".jpg", dir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                captureUri = uri.toString()
                camera.launch(uri)
            } catch (_: Exception) { onError() }
        }) { Text(stringResource(R.string.take_photo)) }
    }

    if (latitude != null && longitude != null) {
        Text(stringResource(R.string.coords_value, latitude!!, longitude!!))
        accuracy?.let {
            if (it > 30f) Text(stringResource(if (it > 50f) R.string.low_accuracy else R.string.approximate))
        }
        TextButton(onClick = { latitude = null; longitude = null; accuracy = null; capturedAt = null; provider = null }) {
            Text(stringResource(R.string.remove_coords))
        }
    }
    LocationButton(onError = { locationMessage = R.string.location_unavailable }, onPermissionResult = { granted ->
        if (!granted) locationMessage = R.string.location_denied
        else if (!locating) {
            locating = true
            scope.launch {
                try {
                    val fix: OneShotLocation.Fix? = model.captureLocation()
                    if (fix != null) {
                        latitude = fix.latitude; longitude = fix.longitude; accuracy = fix.accuracy
                        capturedAt = fix.time; provider = fix.provider
                        locationMessage = if ((fix.accuracy ?: 100f) > 50f) R.string.low_accuracy else null
                    } else locationMessage = R.string.location_unavailable
                } catch (_: Exception) { locationMessage = R.string.location_unavailable }
                finally { locating = false }
            }
        }
    })
    if (locating) LinearProgressIndicator(Modifier.fillMaxWidth())
    locationMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    Text(stringResource(R.string.location_optional), style = MaterialTheme.typography.bodySmall)

    val canSave = !working && !importing && !locating &&
        (name.isNotBlank() || floor.isNotBlank() || sector.isNotBlank() ||
            space.isNotBlank() || note.isNotBlank() || pictures.isNotEmpty() || latitude != null)
    Button(enabled = canSave, modifier = Modifier.fillMaxWidth(), onClick = {
        val time = System.currentTimeMillis()
        model.save(ParkingRecord(
            id = initial?.id ?: UUID.randomUUID().toString(), createdAt = initial?.createdAt ?: time,
            updatedAt = time, placeName = name.trim(), floor = floor.trim(),
            sector = sector.trim(), spaceNumber = space.trim(), note = note.trim(),
            latitude = latitude, longitude = longitude, locationAccuracyMeters = accuracy,
            locationCapturedAt = capturedAt, locationProvider = provider, photos = pictures
        )) { success -> if (success) onSaved() else onError() }
    }) { Text(stringResource(R.string.save)) }
    TextButton(enabled = !working && !importing, onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun ParkingField(label: String, value: String, max: Int, multi: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.take(max)) }, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = !multi, minLines = if (multi) 3 else 1)
}

@Composable
private fun PhotoPreview(file: File, onRemove: (() -> Unit)?) {
    var enlarged by remember { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            if (!file.isFile) null else try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                    var sample = 1
                    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            } catch (_: OutOfMemoryError) { null }
              catch (_: Exception) { null }
        }
    }
    if (bitmap == null) Text(stringResource(R.string.photo_unavailable)) else {
        Image(bitmap!!.asImageBitmap(), contentDescription = stringResource(R.string.photo_description),
            modifier = Modifier.fillMaxWidth().height(180.dp).clickable { enlarged = true },
            contentScale = ContentScale.Fit)
    }
    onRemove?.let { action ->
        TextButton(onClick = action) { Text(stringResource(R.string.remove_photo)) }
    }
    if (enlarged && bitmap != null) DialogPhoto(bitmap!!) { enlarged = false }
}

@Composable
private fun DialogPhoto(bitmap: Bitmap, close: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    AlertDialog(onDismissRequest = close, confirmButton = {
        TextButton(onClick = close) { Text(stringResource(R.string.close)) }
    }, text = {
        Image(bitmap.asImageBitmap(), contentDescription = stringResource(R.string.photo_description),
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).pointerInput(Unit) {
                detectTransformGestures { _, _, scale, _ -> zoom = (zoom * scale).coerceIn(1f, 5f) }
            }.graphicsLayer(scaleX = zoom, scaleY = zoom), contentScale = ContentScale.Fit)
    })
}
