package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable

interface PermissionsController {
    @Throws(Exception::class)
    suspend fun providePermission(permission: Permission)
}

@Composable
expect fun buildPermissionsController(): PermissionsController

enum class Permission {
    RemoteNotification,
}

abstract class PermissionException : Exception()

class PermissionDeniedException : PermissionException()

class PermissionDeniedAlwaysException : PermissionException()

class PermissionRequestCanceledException : PermissionException()
