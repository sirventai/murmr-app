package uoc.edu.psirvent.murmr.presentation.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uoc.edu.psirvent.murmr.domain.model.Contact
import uoc.edu.psirvent.murmr.domain.model.ContactStatus
import uoc.edu.psirvent.murmr.presentation.components.CollapsingHeaderOverlay
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.format.formatChatListTime
import uoc.edu.psirvent.murmr.presentation.screen.home.LocalBottomBarHeight
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactsUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.ContactsViewModel

@Composable
fun ContactsContent(onOpenContact: (contactId: String) -> Unit) {
    val viewModel = rememberKoinInject<ContactsViewModel>()
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val bottomBarHeight = LocalBottomBarHeight.current

    Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = bottomBarHeight),
            ) {
                item("title") {
                    Text(
                        "Contactos",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MurmrColors.OnSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
                item("search") { SearchBar(query = query, onQueryChange = { query = it }) }
                when (val s = state) {
                    ContactsUiState.Loading -> item("loading") {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cargando...", color = MurmrColors.OnSurfaceMuted)
                        }
                    }
                    ContactsUiState.Empty -> item("empty") {
                        Box(modifier = Modifier.fillParentMaxSize()) { EmptyState() }
                    }
                    is ContactsUiState.Loaded -> {
                        val q = query.trim().lowercase()
                        val visible = if (q.isEmpty()) s.contacts
                            else s.contacts.filter { it.displayAlias.lowercase().contains(q) }
                        if (visible.isEmpty()) {
                            item("nomatch") {
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Sin coincidencias", color = MurmrColors.OnSurfaceMuted)
                                }
                            }
                        } else {
                            items(
                                count = visible.size,
                                key = { idx -> visible[idx].contactId },
                            ) { i ->
                                ContactRow(visible[i], onClick = { onOpenContact(visible[i].contactId) })
                            }
                        }
                    }
                }
            }
            CollapsingHeaderOverlay(title = "Contactos", listState = listState)
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MurmrColors.SurfaceDark)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MurmrIcons.Search,
            null,
            tint = MurmrColors.OnSurfaceMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(MurmrColors.PrimaryViolet),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = MurmrColors.OnSurface,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Buscar...",
                            color = MurmrColors.OnSurfaceFaint,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MurmrAvatar(initial = contact.displayAlias, size = 44.dp, avatar = contact.avatar)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.displayAlias,
                color = MurmrColors.OnSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${statusLabel(contact.status)} - ${formatChatListTime(contact.lastMessageAt ?: contact.pairedAt)}",
                color = MurmrColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            )
        }
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MurmrIcons.InfoOutline,
                contentDescription = null,
                tint = MurmrColors.OnSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun statusLabel(status: ContactStatus): String = when (status) {
    ContactStatus.PENDING -> "Pendiente"
    ContactStatus.UNVERIFIED -> "Sin verificar"
    ContactStatus.VERIFIED -> "Verificado"
    ContactStatus.EXPIRED -> "Sesión expirada"
    ContactStatus.BLOCKED -> "Bloqueado"
}

@Composable
private fun EmptyState() {

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.25f))
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MurmrColors.SurfaceDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MurmrIcons.PersonOutline, contentDescription = null, tint = MurmrColors.OnSurfaceMuted, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Sin contactos todavía",
            color = MurmrColors.OnSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Empareja con otro teléfono desde la pestaña Emparejar.",
            color = MurmrColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(0.75f))
    }
}
