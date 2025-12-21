package io.nubit.cloudmusic.designsystem.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.nubit.cloudmusic.designsystem.R

@Composable
fun SongItem(
    imageUrl: String,
    songName: String,
    artistName: List<String>,
    albumName: String,
    duration: String? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit = { },
    onDownloadClick: () -> Unit = { },
    onAddToPlaylistClick: () -> Unit = { },
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 0.dp)
    ) {
        Log.d("COVER", "Image URL: $imageUrl")
        AsyncImage(
            model = imageUrl,
            placeholder = painterResource(R.drawable.default_cover),
            fallback = painterResource(R.drawable.default_cover),
            error = painterResource(R.drawable.default_cover),
            onError = {
                Log.e("COVER", "Error loading image: ${it.result.throwable.message}")
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = songName,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artistName.joinToString(", "),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    painterResource(R.drawable.ic_more_vert_outlined_24dp),
                    contentDescription = "Opciones"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Download") },
                    onClick = {
                        expanded = false
                        onDownloadClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        expanded = false
                        onAddToPlaylistClick()
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SongItemPreview() {
    SongItem(
        imageUrl = "https://example.com/image.jpg",
        songName = "Song Name",
        artistName = listOf("Artist 1", "Artist 2"),
        albumName = "Album Name",
    )
}