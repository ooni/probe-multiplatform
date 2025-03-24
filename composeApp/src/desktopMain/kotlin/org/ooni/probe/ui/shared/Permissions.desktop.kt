package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable

@Composable
actual fun buildPermissionsController() =
    object : PermissionsController {
        override suspend fun providePermission(permission: Permission) {
            // No-op
        }
    }
