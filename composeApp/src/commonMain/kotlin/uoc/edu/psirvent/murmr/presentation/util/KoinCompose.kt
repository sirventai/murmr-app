package uoc.edu.psirvent.murmr.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.core.parameter.ParametersHolder
import org.koin.mp.KoinPlatform

@Composable
inline fun <reified T : Any> rememberKoinInject(): T =
    remember { KoinPlatform.getKoin().get<T>() }

@Composable
inline fun <reified T : Any> rememberKoinInject(noinline parameters: () -> ParametersHolder): T =
    remember { KoinPlatform.getKoin().get<T>(parameters = parameters) }
