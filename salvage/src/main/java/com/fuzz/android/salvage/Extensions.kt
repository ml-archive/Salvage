package com.fuzz.android.salvage

import android.os.Bundle

val <T : Any>  T.bundlePersister: BundlePersister<T>
    get() = Salvager.getBundlePersister(javaClass)

fun <T : Any> T?.onSaveInstanceState(bundle: Bundle) = Salvager.onSaveInstanceState(this, bundle)

fun <T : Any> T?.onRestoreInstanceState(bundle: Bundle) = Salvager.onRestoreInstanceState(this, bundle)

fun <T : Any> T.loadArguments(bundle: Bundle?) = Salvager.loadArguments(this, bundle)