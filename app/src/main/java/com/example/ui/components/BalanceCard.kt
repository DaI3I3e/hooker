package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkGreenGradientEnd
import com.example.ui.theme.DarkGreenPrimary
import com.example.util.AmountFormatter

@Composable
fun BalanceCard(
    totalBalance: Long,
    modifier: Modifier = Modifier
) {
    var isBalanceHidden by rememberSaveable { mutableStateOf(true) }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(DarkGreenPrimary, DarkGreenGradientEnd)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = DarkGreenPrimary.copy(alpha = 0.45f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
        ) {
            // Elegant background ambient geometry
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = h * 0.9f,
                    center = Offset(w * 0.9f, h * 0.1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = h * 0.6f,
                    center = Offset(w * 0.1f, h * 0.9f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "موجودی کل حساب‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { isBalanceHidden = !isBalanceHidden },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBalanceHidden) "نمایش موجودی" else "مخفی کردن موجودی",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = isBalanceHidden,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "balanceAnim"
                ) { hidden ->
                    if (hidden) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "••••••••",
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ریال",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = AmountFormatter.format(totalBalance, includeCurrency = false),
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ریال",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
