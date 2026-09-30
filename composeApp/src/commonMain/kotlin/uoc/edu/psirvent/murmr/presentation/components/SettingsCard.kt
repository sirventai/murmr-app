package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MurmrColors.SurfaceDark)
            .border(0.5.dp, MurmrColors.DividerDark, RoundedCornerShape(12.dp)),
    ) { content() }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val baseTint = if (danger) MurmrColors.Error else MurmrColors.OnSurface
    val tint = if (enabled) baseTint else MurmrColors.OnSurfaceFaint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = tint,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        )
        if (value != null) {
            Text(
                value,
                color = if (enabled) MurmrColors.OnSurfaceMuted else MurmrColors.OnSurfaceFaint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = MurmrIcons.ChevronRight,
            contentDescription = null,
            tint = MurmrColors.OnSurfaceFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MurmrColors.OnSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MurmrColors.OnSurface,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MurmrColors.PrimaryViolet,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MurmrColors.DividerDark,
            ),
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .height(0.5.dp)
            .background(MurmrColors.DividerDark),
    )
}
