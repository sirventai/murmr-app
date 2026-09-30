package uoc.edu.psirvent.murmr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.data.network.AppSession
import uoc.edu.psirvent.murmr.data.network.ConnectionState
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject

@Composable
fun ConnectionStatusBadge(modifier: Modifier = Modifier) {
    val session = rememberKoinInject<AppSession>()
    val relayBus = rememberKoinInject<RelayDialogBus>()
    val state by session.state.collectAsState()

    val (dotColor, label) = when (state) {
        ConnectionState.Connected -> MurmrColors.Success to "Conectado"
        ConnectionState.Connecting -> MurmrColors.AccentCyan to "Conectando..."
        ConnectionState.Disconnected -> MurmrColors.Error to "Desconectado"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MurmrColors.SurfaceDark)
            .clickable { relayBus.fire() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = MurmrColors.OnSurfaceMuted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        )
    }
}
