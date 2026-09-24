package hu.infokristaly.homework4timersonandroid.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

fun createLocalizedContext(context: Context, languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val res = context.resources
    val config = Configuration(res.configuration)
    config.setLocale(locale)

    @Suppress("DEPRECATION")
    res.updateConfiguration(config, res.displayMetrics)

    return context.createConfigurationContext(config)
}

@Composable
fun LocalizedApp(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val localizedContext = remember(currentContext, languageCode) {
        createLocalizedContext(currentContext, languageCode)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        content = content
    )
}
