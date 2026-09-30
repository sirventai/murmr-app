package uoc.edu.psirvent.murmr.presentation.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.presentation.util.rememberKoinInject
import androidx.compose.foundation.clickable
import uoc.edu.psirvent.murmr.presentation.components.IllustrationLockedPhone
import uoc.edu.psirvent.murmr.presentation.components.IllustrationSoundExchange
import uoc.edu.psirvent.murmr.presentation.components.IllustrationVerifiedShield
import uoc.edu.psirvent.murmr.presentation.components.MurmrAvatar
import uoc.edu.psirvent.murmr.presentation.components.MurmrIcons
import uoc.edu.psirvent.murmr.presentation.components.rememberAvatarPicker
import uoc.edu.psirvent.murmr.presentation.screen.home.HomeScreen
import uoc.edu.psirvent.murmr.presentation.theme.MurmrColors
import uoc.edu.psirvent.murmr.presentation.util.UiTextos
import uoc.edu.psirvent.murmr.presentation.viewmodel.OnboardingUiState
import uoc.edu.psirvent.murmr.presentation.viewmodel.OnboardingViewModel
import uoc.edu.psirvent.murmr.util.ValidationResult
import uoc.edu.psirvent.murmr.util.Validators
import uoc.edu.psirvent.murmr.util.compressAvatar

private data class Slide(
    val title: String,
    val description: String,
    val illustration: @Composable () -> Unit,
)

private val slides = listOf(
    Slide(
        title = "Mensajería cifrada de verdad",
        description = "Tus mensajes se cifran en tu móvil y solo se descifran en el destinatario. Nadie más puede leerlos. Ni nosotros.",
        illustration = { IllustrationLockedPhone(size = 220.dp) },
    ),
    Slide(
        title = "Claves por sonido",
        description = "Para activar el cifrado, Murmr intercambia las claves mediante sonido de alta frecuencia entre dos móviles cercanos. Sin WiFi. Sin Bluetooth. Sin servidores.",
        illustration = { IllustrationSoundExchange(size = 220.dp) },
    ),
    Slide(
        title = "Verifica con emojis",
        description = "Tras el intercambio, ambos móviles deberán introducir una secuencia de emojis segura. Si coinciden, nadie ha interceptado la conexión. Así de simple.",
        illustration = { IllustrationVerifiedShield(size = 220.dp) },
    ),
)

object OnboardingScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberKoinInject<OnboardingViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            viewModel.state.collectLatest { s ->
                if (s is OnboardingUiState.Completed) navigator.replaceAll(HomeScreen)
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MurmrColors.BackgroundDark) {
            when (val current = state) {
                is OnboardingUiState.InProgress -> InProgressContent(current, viewModel)
                OnboardingUiState.Submitting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MurmrColors.PrimaryViolet)
                }
                OnboardingUiState.Completed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bienvenido", color = MurmrColors.OnSurface)
                }
                is OnboardingUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error", style = MaterialTheme.typography.headlineMedium, color = MurmrColors.Error)
                        Text(current.message, color = MurmrColors.OnSurfaceMuted)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetFromError() }) { Text(UiTextos.Reintentar) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InProgressContent(state: OnboardingUiState.InProgress, vm: OnboardingViewModel) {
    val totalPages = slides.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages }, initialPage = state.pageIndex)
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.size

    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) pagerState.animateScrollToPage(state.pageIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.pageIndex) vm.onPageChange(pagerState.currentPage)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 56.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) { page ->
            if (page < slides.size) SlideContent(slides[page]) else AliasFormContent(state, vm)
        }

        Spacer(Modifier.height(24.dp))
        PageDots(current = pagerState.currentPage, total = totalPages)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (isLast) vm.submit()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            enabled = !isLast || (state.alias.isNotBlank() && state.aliasError == null),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MurmrColors.PrimaryViolet,
                contentColor = Color.White,
                disabledContainerColor = MurmrColors.PrimaryViolet.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
        ) {
            Text(
                if (isLast) "Finalizar" else "Siguiente",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun SlideContent(slide: Slide) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        slide.illustration()
        Spacer(Modifier.height(48.dp))
        Text(
            slide.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
            color = MurmrColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            slide.description,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = MurmrColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AliasFormContent(state: OnboardingUiState.InProgress, vm: OnboardingViewModel) {
    val avatarPicker = rememberAvatarPicker { bytes ->
        if (bytes == null) return@rememberAvatarPicker

        when (Validators.avatarBytes(bytes)) {
            ValidationResult.Ok -> {
                val thumb = compressAvatar(bytes, maxSidePx = 256, quality = 85)
                val big = compressAvatar(bytes, maxSidePx = 1024, quality = 88)
                vm.onAvatarChange(thumb, big)
            }
            is ValidationResult.Invalid -> Unit
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Información de perfil",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
            color = MurmrColors.OnSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Por favor, proporciona tu nombre y una foto de perfil opcional",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MurmrColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.clickable { avatarPicker() },
        ) {
            MurmrAvatar(
                size = 120.dp,
                initial = state.alias.takeIf { it.isNotBlank() },
                avatar = state.avatar,
            )
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(MurmrColors.SurfaceDark),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MurmrIcons.Edit,
                    contentDescription = "Editar foto",
                    tint = MurmrColors.OnSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = state.alias,
            onValueChange = vm::onAliasChange,
            placeholder = { Text("Introduce tu nombre aquí...", color = MurmrColors.OnSurfaceFaint) },
            singleLine = true,
            isError = state.aliasError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                Icon(
                    imageVector = MurmrIcons.Mood,
                    contentDescription = null,
                    tint = MurmrColors.OnSurfaceFaint,
                    modifier = Modifier.size(18.dp),
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MurmrColors.OnSurface,
                unfocusedTextColor = MurmrColors.OnSurface,
                focusedContainerColor = MurmrColors.SurfaceDark,
                unfocusedContainerColor = MurmrColors.SurfaceDark,
                focusedIndicatorColor = MurmrColors.DividerDark,
                unfocusedIndicatorColor = MurmrColors.DividerDark,
                cursorColor = MurmrColors.PrimaryViolet,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(state.aliasError != null) {
            Text(
                state.aliasError.orEmpty(),
                color = MurmrColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PageDots(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) MurmrColors.OnSurface else MurmrColors.OnSurfaceFaint),
            )
        }
    }
}
