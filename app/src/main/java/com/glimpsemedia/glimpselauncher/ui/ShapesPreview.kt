package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.SHAPE_CIRCLE
import com.glimpsemedia.glimpselauncher.data.SHAPE_DEFAULT

/** The two choices every shape setting offers. */
val SHAPE_CHOICES = listOf(SHAPE_DEFAULT to "Default", SHAPE_CIRCLE to "Circle")

/**
 * A small picture of two places on the screen, in the shapes chosen now: a tile with its icon, and an empty place.
 * Numbers mark the three parts the shape settings below change, so it is clear which is which.
 */
@Composable
fun ShapesPreview(showTiles: Boolean) {
    val palette = LocalPalette.current
    val tileShape = tileShapeOf(LocalTileShape.current, 22.dp)
    val iconShape = iconSquareShape(LocalIconShape.current, 56.dp)
    val slotShape = tileShapeOf(LocalSlotShape.current, 22.dp)
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "Preview of the shapes: 1 the tile, 2 the icon background, 3 the empty place"
    }) {
        // A dark stand-in for the wallpaper, so the white outline of the empty place shows on light menus too.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2E4A5C), Color(0xFF16242E))))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // ① the tile, holding ② the icon background
            Box(Modifier.size(100.dp, 118.dp)) {
                Column(
                    Modifier.fillMaxSize().clip(tileShape)
                        .then(if (showTiles) Modifier.background(palette.tile.copy(alpha = 0.9f)) else Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), tileShape)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(56.dp).clip(iconShape).background(palette.icon.copy(alpha = 1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = readableOn(palette.icon.copy(alpha = 1f)), modifier = Modifier.size(32.dp))
                    }
                    Text("App", color = if (showTiles) readableOn(palette.tile.copy(alpha = 1f)) else Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                PartMark("1", Modifier.align(Alignment.TopStart))
                PartMark("2", Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 4.dp))
            }
            // ③ the empty place
            Box(Modifier.size(100.dp, 118.dp)) {
                Box(
                    Modifier.fillMaxSize().clip(slotShape).background(Color.White.copy(alpha = 0.12f))
                        .border(2.dp, Color.White.copy(alpha = 0.5f), slotShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                PartMark("3", Modifier.align(Alignment.TopStart))
            }
        }
        Column(Modifier.padding(top = 8.dp)) {
            Legend("1", "Tile: the card behind an icon and its name")
            Legend("2", "Icon background: the square behind each icon")
            Legend("3", "Empty place: shows with a + while unlocked")
        }
    }
}

@Composable
private fun PartMark(n: String, modifier: Modifier = Modifier) =
    Box(modifier.size(22.dp).clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        Text(n, color = readableOn(LocalAccent.current), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

@Composable
private fun Legend(n: String, text: String) = Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    PartMark(n)
    Text(text, color = LocalMenu.current.text, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
}
