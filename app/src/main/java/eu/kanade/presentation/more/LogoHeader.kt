package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import niaromi.icons.materialsymbols.rounded.Book

@Composable
fun LogoHeader(
    iconPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(iconPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(Color(0xFF1D1D1D), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Book,
                contentDescription = null,
                tint = Color(0xFFFFFDF4),
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "NIAROMI",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF171717),
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Editorial Manga Reader",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color(0xFF888888),
            ),
        )

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFE8E4D5),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
