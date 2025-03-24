package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

@Composable
actual fun buildPermissionsController(): PermissionsController {
    val mokoFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val mokoController = remember(mokoFactory) { mokoFactory.createPermissionsController() }
    BindEffect(mokoController)
    val controller = remember(mokoController) {
        object : PermissionsController {
            override suspend fun providePermission(permission: Permission) {
                try {
                    mokoController.providePermission(
                        when (permission) {
                            Permission.RemoteNotification ->
                                dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION
                        },
                    )
                } catch (e: DeniedException) {
                    throw PermissionDeniedException()
                } catch (e: DeniedAlwaysException) {
                    throw PermissionDeniedAlwaysException()
                } catch (e: RequestCanceledException) {
                    throw PermissionRequestCanceledException()
                }
            }
        }
    }
    return controller
}
