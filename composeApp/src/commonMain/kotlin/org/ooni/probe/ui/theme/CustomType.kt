package org.ooni.probe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography.cardTitle
    @Composable
    get() = MaterialTheme.typography.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    )

val Typography.dashboardSectionTitle
    @Composable
    get() = MaterialTheme.typography.titleMedium.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )
