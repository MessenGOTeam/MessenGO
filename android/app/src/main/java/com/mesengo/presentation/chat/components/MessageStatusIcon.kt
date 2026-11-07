package com.mesengo.presentation.chat.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mesengo.R

@Composable
fun MessageStatusIcon(
    status: String,
    isSeen: Boolean,
    modifier: Modifier = Modifier
) {
    val seenColor = Color(0xFF2563EB) // Blue seen color
    val greyColor = Color(0xFF94A3B8) // Slate grey

    when {
        isSeen -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_status_delivered),
                contentDescription = "Seen",
                tint = seenColor,
                modifier = modifier.size(15.dp)
            )
        }
        status == "delivered" -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_status_delivered),
                contentDescription = "Delivered",
                tint = greyColor,
                modifier = modifier.size(15.dp)
            )
        }
        status == "sent" -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_status_sent),
                contentDescription = "Sent",
                tint = greyColor,
                modifier = modifier.size(14.dp)
            )
        }
        else -> {
            // "pending" in Outbox
            Icon(
                painter = painterResource(id = R.drawable.ic_status_pending),
                contentDescription = "Pending Outbox",
                tint = greyColor,
                modifier = modifier.size(13.dp)
            )
        }
    }
}
