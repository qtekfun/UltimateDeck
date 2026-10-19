// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.ui.localnet.HostResolver
import com.qtekfun.ultimatedeck.ui.localnet.LocalNetwork
import com.qtekfun.ultimatedeck.ui.localnet.LocalNetworkGranted
import com.qtekfun.ultimatedeck.ui.localnet.LocalNetworkPermission
import com.qtekfun.ultimatedeck.ui.localnet.SystemHostResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** The system's side of the local network permission (Android 17+): the check and the resolver. */
@Module
@InstallIn(SingletonComponent::class)
object LocalNetworkModule {
    @Provides
    fun permission(@ApplicationContext context: Context): LocalNetworkPermission =
        LocalNetworkPermission {
            // Before Android 17 the permission does not exist and local access is free.
            Build.VERSION.SDK_INT < LocalNetwork.FIRST_SDK ||
                ContextCompat.checkSelfPermission(context, LocalNetwork.PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        }

    @Provides
    fun resolver(): HostResolver = SystemHostResolver()

    /** Once the local network is allowed again, sync at once instead of after the retry delay. */
    @Provides
    fun granted(scheduler: SyncScheduler): LocalNetworkGranted =
        LocalNetworkGranted { scheduler.requestSync() }
}
