package uoc.edu.psirvent.murmr.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(extra: KoinAppDeclaration? = null) {
    startKoin {
        extra?.invoke(this)
        modules(
            appModule,
            dataModule,
            domainModule,
            platformModule(),
        )
    }
}
