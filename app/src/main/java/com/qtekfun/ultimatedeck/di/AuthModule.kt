// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import com.qtekfun.ultimatedeck.data.auth.AndroidKeystoreCipher
import com.qtekfun.ultimatedeck.data.auth.SecretCipher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    /** Credentials are encrypted with a key that lives in Android Keystore. */
    @Binds
    abstract fun secretCipher(cipher: AndroidKeystoreCipher): SecretCipher
}
