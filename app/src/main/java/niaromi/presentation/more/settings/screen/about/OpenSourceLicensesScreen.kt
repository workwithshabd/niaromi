package niaromi.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import niaromi.R
import niaromi.core.navigation.LocalNavigator
import niaromi.presentation.components.AppBar
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR

@Composable
fun OpenSourceLicensesScreen() {
    val navigator = LocalNavigator.current
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.licenses),
                navigateUp = navigator::pop,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        val libraries by produceLibraries(R.raw.aboutlibraries)
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = contentPadding,
            detailMode = LibraryDetailMode.Sheet,
        )
    }
}
