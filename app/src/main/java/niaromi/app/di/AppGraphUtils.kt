package niaromi.app.di

import android.content.Context
import niaromi.core.metro.metroGraph

val Context.appGraph get() = metroGraph<AppGraph>()
