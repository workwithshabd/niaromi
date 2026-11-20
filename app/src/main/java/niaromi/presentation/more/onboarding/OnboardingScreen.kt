package niaromi.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import niaromi.app.di.appGraph
import niaromi.domain.storage.service.autoResolveOrCreateDirectory
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.RocketLaunch
import niaromi.presentation.core.components.material.padding
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.core.screens.InfoScreen
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.MR

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: (String) -> Unit,
) {
    val context = LocalContext.current
    val slideDistance = rememberSlideDistance()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val storagePref = context.appGraph.storagePreferences.baseStorageDirectory
        if (!storagePref.isSet()) {
            val autoDir = autoResolveOrCreateDirectory(context)
            if (autoDir != null) {
                storagePref.set(android.net.Uri.fromFile(autoDir).toString())
            }
        }
    }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            PermissionStep(),
            RestoreStep(onRestoreLocal = onRestoreBackup),
        )
    }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0) {
        currentStep--
    }

    InfoScreen(
        icon = MaterialSymbols.Rounded.RocketLaunch,
        headingText = stringResource(MR.strings.onboarding_heading),
        subtitleText = stringResource(MR.strings.onboarding_description),
        acceptText = stringResource(
            if (isLastStep) {
                MR.strings.onboarding_action_finish
            } else {
                MR.strings.onboarding_action_next
            },
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.small)
                .clip(MaterialTheme.shapes.small)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
            ) {
                steps[it].Content()
            }
        }
    }
}
