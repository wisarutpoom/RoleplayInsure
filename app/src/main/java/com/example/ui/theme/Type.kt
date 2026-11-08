package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PromptFontFamily =
  FontFamily(
    Font(R.font.prompt, FontWeight.Normal),
    Font(R.font.prompt, FontWeight.Medium),
    Font(R.font.prompt, FontWeight.SemiBold),
    Font(R.font.prompt, FontWeight.Bold),
    Font(R.font.prompt, FontWeight.ExtraBold),
  )

val SarabunFontFamily =
  FontFamily(
    Font(R.font.sarabun, FontWeight.Normal),
    Font(R.font.sarabun, FontWeight.Medium),
    Font(R.font.sarabun, FontWeight.SemiBold),
    Font(R.font.sarabun, FontWeight.Bold),
  )

val Typography =
  Typography(
    headlineMedium =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 27.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = SarabunFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = SarabunFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = SarabunFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
      ),
  )
