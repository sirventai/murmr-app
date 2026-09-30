package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberAvatarPicker(onResult: (ByteArray?) -> Unit): () -> Unit
