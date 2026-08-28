package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MovementType
import com.example.domain.model.StockStatus
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.theme.MinimalBlueContainer
import com.example.ui.theme.MinimalOnBlueContainer
import com.example.ui.theme.Red100
import com.example.ui.theme.Red700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700

@Composable
fun StockStatusBadge(
    status: StockStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        StockStatus.NORMAL -> Pair(Emerald100, Emerald700)
        StockStatus.LOW_STOCK -> Pair(Amber100, Amber700)
        StockStatus.OUT_OF_STOCK -> Pair(Red100, Red700)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.emoji,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun MovementBadge(
    type: MovementType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        MovementType.COMPRA -> Pair(Emerald100, Emerald700)
        MovementType.VENTA -> Pair(MinimalBlueContainer, MinimalOnBlueContainer)
        MovementType.AJUSTE -> Pair(Amber100, Amber700)
        MovementType.PERDIDA -> Pair(Red100, Red700)
        MovementType.INICIAL -> Pair(Slate100, Slate700)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type.emoji,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}
