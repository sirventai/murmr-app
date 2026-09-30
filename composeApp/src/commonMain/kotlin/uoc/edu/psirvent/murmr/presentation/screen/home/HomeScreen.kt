package uoc.edu.psirvent.murmr.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.data.repository.ContactRepository
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import uoc.edu.psirvent.murmr.presentation.components.ConnectionStatusBadge
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.components.RelayDialogBus
import uoc.edu.psirvent.murmr.presentation.screen.chat.ChatScreen
import uoc.edu.psirvent.murmr.presentation.screen.chat.ChatsListContent
import uoc.edu.psirvent.murmr.presentation.screen.contacts.ContactDetailScreen
import uoc.edu.psirvent.murmr.presentation.screen.contacts.ContactsContent
import uoc.edu.psirvent.murmr.presentation.screen.pairing.PairTabContent
import uoc.edu.psirvent.murmr.presentation.screen.settings.SettingsContent
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors

val LocalRootNavigator: ProvidableCompositionLocal<Navigator?> = compositionLocalOf { null }

val LocalBottomBarHeight: ProvidableCompositionLocal<Dp> = compositionLocalOf { 0.dp }

object HomeScreen : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private var processSavedPage by mutableStateOf(0)

    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        val pagerState = rememberPagerState(initialPage = processSavedPage, pageCount = { TAB_COUNT })
        val scope = rememberCoroutineScope()
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { processSavedPage = it }
        }

        val relayBus = rememberKoinInject<RelayDialogBus>()
        val relayTrigger by relayBus.open.collectAsState()
        LaunchedEffect(relayTrigger) {
            if (relayTrigger > 0) pagerState.animateScrollToPage(TAB_SETTINGS)
        }
        LaunchedEffect(Unit) {
            HomeTabRouter.requestedTab.collect { req ->
                val target = when (req) {
                    HomeTab.Chats -> TAB_CHATS
                    HomeTab.Pairing -> TAB_PAIR
                    HomeTab.Contacts -> TAB_CONTACTS
                    HomeTab.Settings -> TAB_SETTINGS
                }
                runCatching { pagerState.animateScrollToPage(target) }
            }
        }

        val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomBarHeight = BOTTOM_BAR_CONTENT_HEIGHT + navInsetBottom

        CompositionLocalProvider(
            LocalRootNavigator provides rootNavigator,
            LocalBottomBarHeight provides bottomBarHeight,
        ) {

            Box(modifier = Modifier.fillMaxSize().background(MurmrColors.BackgroundDark)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        TAB_CHATS -> ChatsTabContent()
                        TAB_PAIR -> PairTabContent()
                        TAB_CONTACTS -> ContactsTabContent()
                        TAB_SETTINGS -> SettingsContent()
                    }
                }
                MurmrBottomBar(
                    currentPage = pagerState.currentPage,
                    onTabClick = { idx ->
                        scope.launch { pagerState.animateScrollToPage(idx) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                ConnectionStatusBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 18.dp),
                )
            }
        }
    }
}

private const val TAB_CHATS = 0
private const val TAB_PAIR = 1
private const val TAB_CONTACTS = 2
private const val TAB_SETTINGS = 3
private const val TAB_COUNT = 4

private val BOTTOM_BAR_CONTENT_HEIGHT = 64.dp

@Composable
private fun ChatsTabContent() {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.currentOrThrow
    ChatsListContent(onOpenChat = { contactId -> rootNavigator.push(ChatScreen(contactId)) })
}

@Composable
private fun ContactsTabContent() {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.currentOrThrow
    ContactsContent(onOpenContact = { contactId -> rootNavigator.push(ContactDetailScreen(contactId)) })
}

private data class TabItem(val index: Int, val icon: ImageVector, val label: String)

private val TABS = listOf(
    TabItem(TAB_CHATS, MurmrIcons.ChatBubbleOutline, "Chats"),
    TabItem(TAB_PAIR, MurmrIcons.GraphicEq, "Emparejar"),
    TabItem(TAB_CONTACTS, MurmrIcons.PersonOutline, "Contactos"),
    TabItem(TAB_SETTINGS, MurmrIcons.Settings, "Ajustes"),
)

@Composable
private fun MurmrBottomBar(
    currentPage: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contactRepo = rememberKoinInject<ContactRepository>()
    val totalUnread by contactRepo.observeTotalUnread().collectAsState(initial = 0)

    Surface(
        color = MurmrColors.BackgroundDark.copy(alpha = 0.85f),
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MurmrColors.DividerDark.copy(alpha = 0.6f)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BOTTOM_BAR_CONTENT_HEIGHT)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TABS.forEach { tab ->
                    val selected = currentPage == tab.index
                    BottomBarItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = selected,
                        badgeCount = if (tab.index == TAB_CHATS) totalUnread else 0,
                        onClick = { onTabClick(tab.index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val color = if (selected) MurmrColors.PrimaryViolet else MurmrColors.OnSurfaceFaint
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Box {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-4).dp)
                            .size(if (badgeCount > 9) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(MurmrColors.Error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = color,
            )
        }
    }
}
