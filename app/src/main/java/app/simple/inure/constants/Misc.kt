package app.simple.inure.constants

import android.graphics.Color
import androidx.dynamicanimation.animation.SpringForce

object Misc {
    const val delay = 500L
    const val roundedCornerFactor = 1.5F
    const val maxBlur = 10F
    const val minBlur = 0.1F
    const val blurRadius = 16F
    const val dimAmount = 0.75F
    const val highlightColorAlpha = 0.15F

    const val COLOR_PICKER_INDEX = 2

    const val ONE = 1
    const val TWO = 2
    const val THREE = 3
    const val FOUR = 4
    const val FIVE = 5
    const val SIX = 6
    const val SEVEN = 7
    const val EIGHT = 8
    const val NINE = 9

    var xOffset = 0F
    var yOffset = 0F

    // Hover props
    const val hoverAnimationDuration = 250L
    const val hoverAnimationDampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
    const val hoverAnimationStiffness = SpringForce.STIFFNESS_LOW
    const val hoverAnimationScaleOnHover = 0.90F
    const val hoverAnimationScaleOnUnHover = 1.0F
    const val hoverAnimationElevation = 10F
    const val hoverAnimationAlpha = 0.8F

    // Misc
    const val SHIZUKU_CODE = 654

    const val splitApkFormat = ".apks"
    const val apkFormat = ".apk"

    var textHighlightFocused = Color.parseColor("#a2d9ce")
    var textHighlightUnfocused = Color.parseColor("#f9e79f")
    var linkColor = Color.parseColor("#2e86c1")
}
