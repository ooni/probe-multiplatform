package org.ooni.probe.ui.shared

import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

fun requestAppReview() {
    val scene = UIApplication.sharedApplication.connectedScenes
        .mapNotNull { it as? UIWindowScene }
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: return
    SKStoreReviewController.requestReviewInScene(scene)
}
