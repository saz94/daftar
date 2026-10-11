package com.daftar.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.daftar.app.R
import java.io.File

// ---- importing an invoice from a picture

class ScanActions(val camera: () -> Unit, val gallery: () -> Unit)

/**
 * Registers the camera and gallery launchers at screen level, so the result still arrives after
 * the bottom sheet that started them has closed.
 */
@Composable
fun rememberScanActions(vm: AppViewModel, onReady: () -> Unit): ScanActions {
    val context = LocalContext.current
    val ready by rememberUpdatedState(onReady)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPath
        if (saved && path != null) vm.scan(File(path)) { ready() }
    }
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importAndScan(uri) { ready() }
    }
    return remember(takePicture, pickPicture) {
        ScanActions(
            camera = {
                try {
                    val file = vm.newScanFile()
                    pendingPath = file.absolutePath
                    takePicture.launch(FileProvider.getUriForFile(context, context.packageName + ".files", file))
                } catch (e: Exception) {
                    // no camera app on this device; the gallery and manual entry still work
                }
            },
            gallery = {
                pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                stringResource(R.string.add_purchase),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Text(
                stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
            ListRow(
                title = stringResource(R.string.scan_camera),
                subtitle = stringResource(R.string.scan_camera_hint),
                onClick = onCamera,
                leading = { IconBadge(Icons.Outlined.CameraAlt) },
            )
            ListRow(
                title = stringResource(R.string.scan_gallery),
                subtitle = stringResource(R.string.scan_gallery_hint),
                onClick = onGallery,
                leading = { IconBadge(Icons.Outlined.PhotoLibrary) },
            )
            ListRow(
                title = stringResource(R.string.enter_manually),
                subtitle = stringResource(R.string.enter_manually_hint),
                onClick = onManual,
                leading = { IconBadge(Icons.Outlined.Description) },
            )
        }
    }
}
