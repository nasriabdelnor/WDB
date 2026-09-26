package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeldJoint
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenContainer
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.PendingPurple
import com.example.ui.theme.PendingPurpleContainer
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedContainer
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldAmber
import com.example.ui.theme.WeldAmberContainer

@Composable
fun WeldOverallStatusBadge(
    weld: WeldJoint,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text, icon) = when {
        weld.isRepairRequired -> {
            Quad(RejectRedContainer, RejectRedDark, "Réparation Requise", Icons.Default.Warning)
        }
        weld.isPendingNdt -> {
            Quad(PendingPurpleContainer, PendingPurple, "En attente CND (${weld.ndtType})", Icons.Default.HourglassEmpty)
        }
        weld.status == "IN_PROGRESS" || weld.visualStatus == "PENDING" -> {
            Quad(WeldAmberContainer, WeldAmber, "En cours de soudage", Icons.Default.Build)
        }
        else -> {
            Quad(ApprovedGreenContainer, ApprovedGreenDark, "Conforme / Validé", Icons.Default.CheckCircle)
        }
    }

    Row(
        modifier = modifier
            .testTag("status_badge_${weld.jointNo}")
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun InspectionPill(
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val (dotColor, bg) = when (status.uppercase()) {
        "ACCEPTED", "OK", "VALIDE" -> Pair(ApprovedGreen, ApprovedGreenContainer.copy(alpha = 0.5f))
        "REJECTED", "REPAIR", "REFUSE" -> Pair(RejectRed, RejectRedContainer.copy(alpha = 0.5f))
        "NOT_REQUIRED", "N/A" -> Pair(Color.Gray, Color.LightGray.copy(alpha = 0.3f))
        else -> Pair(WeldAmber, WeldAmberContainer.copy(alpha = 0.5f))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
