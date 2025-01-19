package app.simple.inure.text

import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.MaskFilterSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import app.simple.inure.preferences.AppearancePreferences
import app.simple.inure.util.ConditionUtils.invert
import java.util.Objects
import kotlin.math.roundToInt

object EditTextHelper {

    private const val bulletGap = 16
    private const val bulletRadius = 8
    private const val stripWidth = 6
    private const val blurRadius = 5f
    private const val spanUpperThreshold = 96
    private const val spanLowerThreshold = 12
    private const val relativeProportion = 0.75f

    private var leftSpace: Int = 0
    private var rightSpace: Int = 0
    private var cursorPosition: Int = 0

    private var wasAlreadySelected = false

    fun EditText.toBold() {
        selectTheCurrentWord()

        val spans: Array<StyleSpan> = text.getSpans(leftSpace, rightSpace, StyleSpan::class.java)
        var exists = false

        for (span in spans) {
            if (span.style == Typeface.BOLD) {
                text.removeSpan(span)
                exists = true
            }
        }

        if (!exists) {
            text.setSpan(StyleSpan(Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.toItalics() {
        selectTheCurrentWord()

        val spans: Array<StyleSpan> = text.getSpans(selectionStart, selectionEnd, StyleSpan::class.java)
        var exists = false

        for (span in spans) {
            if (span.style == Typeface.ITALIC) {
                text.removeSpan(span)
                exists = true
            }
        }

        if (!exists) {
            text.setSpan(StyleSpan(Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.toUnderline() {
        selectTheCurrentWord()

        val spans: Array<UnderlineSpan> = text.getSpans(selectionStart, selectionEnd, UnderlineSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            text.setSpan(UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.toStrikethrough() {
        selectTheCurrentWord()

        val spans: Array<StrikethroughSpan> = text.getSpans(selectionStart, selectionEnd, StrikethroughSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            text.setSpan(StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.addBullet() {
        val spans: Array<BulletSpan> = text.getSpans(selectionStart, selectionEnd, BulletSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                text.setSpan(BulletSpan(bulletGap, AppearancePreferences.getAccentColor(), bulletRadius),
                             selectionStart, selectionEnd,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                text.setSpan(BulletSpan(bulletGap, AppearancePreferences.getAccentColor()),
                             selectionStart, selectionEnd,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun EditText.toSuperscript() {
        selectTheCurrentWord()

        val superscriptSpan: Array<SuperscriptSpan> = text.getSpans(selectionStart, selectionEnd, SuperscriptSpan::class.java)
        val subscriptSpan: Array<SubscriptSpan> = text.getSpans(selectionStart, selectionEnd, SubscriptSpan::class.java)
        val relativeSizeSpan: Array<RelativeSizeSpan> = text.getSpans(selectionStart, selectionEnd, RelativeSizeSpan::class.java)

        for (subscript in subscriptSpan) {
            text.removeSpan(subscript)
        }

        for (sizeSpan in relativeSizeSpan) {
            text.removeSpan(sizeSpan)
        }

        var exists = false

        for (span in superscriptSpan) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            text.setSpan(SuperscriptSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(RelativeSizeSpan(relativeProportion), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.toSubscript() {
        selectTheCurrentWord()

        val subscriptSpan: Array<SubscriptSpan> = text.getSpans(selectionStart, selectionEnd, SubscriptSpan::class.java)
        val superscriptSpan: Array<SuperscriptSpan> = text.getSpans(selectionStart, selectionEnd, SuperscriptSpan::class.java)
        val relativeSizeSpan: Array<RelativeSizeSpan> = text.getSpans(selectionStart, selectionEnd, RelativeSizeSpan::class.java)

        for (superscript in superscriptSpan) {
            text.removeSpan(superscript)
        }

        for (sizeSpan in relativeSizeSpan) {
            text.removeSpan(sizeSpan)
        }

        var exists = false

        for (span in subscriptSpan) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            text.setSpan(SubscriptSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(RelativeSizeSpan(relativeProportion), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.setTextSize(size: Int) {
        selectTheCurrentWord()

        val spans: Array<AbsoluteSizeSpan> = text.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
        var fontSize: Int = textSize.roundToInt()

        for (span in spans) {
            fontSize = if (span.size > spanUpperThreshold) {
                spanUpperThreshold
            } else {
                size
            }
        }

        text.setSpan(AbsoluteSizeSpan(fontSize), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.resetTextSize() {
        selectTheCurrentWord()

        val spans: Array<AbsoluteSizeSpan> = text.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)

        for (span in spans) {
            text.removeSpan(span)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.getCurrentTextSize(): Int {
        selectTheCurrentWord()

        val spans: Array<AbsoluteSizeSpan> = text.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
        var fontSize: Int = textSize.roundToInt()

        for (span in spans) {
            fontSize = span.size
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }

        return fontSize
    }

    fun EditText.increaseTextSize() {
        val spans: Array<AbsoluteSizeSpan> = text.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
        var fontSize: Int = textSize.roundToInt()

        for (span in spans) {
            fontSize = if (span.size.plus(2) > spanUpperThreshold) {
                spanUpperThreshold
            } else {
                span.size.plus(2)
            }
        }

        text.setSpan(AbsoluteSizeSpan(fontSize), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun EditText.decreaseTextSize() {
        val spans: Array<AbsoluteSizeSpan> = text.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
        var fontSize: Int = textSize.roundToInt()

        for (span in spans) {
            fontSize = if (span.size.minus(2) < spanLowerThreshold) {
                spanLowerThreshold
            } else {
                span.size.minus(2)
            }
        }

        text.setSpan(AbsoluteSizeSpan(fontSize), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun EditText.highlightText(color: Int) {
        selectTheCurrentWord()

        val spans: Array<BackgroundColorSpan> = text.getSpans(selectionStart, selectionEnd, BackgroundColorSpan::class.java)

        for (span in spans) {
            text.removeSpan(span)
        }

        text.setSpan(BackgroundColorSpan(color), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.clearHighlight() {
        selectTheCurrentWord()

        val spans: Array<BackgroundColorSpan> = text.getSpans(selectionStart, selectionEnd, BackgroundColorSpan::class.java)

        for (span in spans) {
            text.removeSpan(span)
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.toQuote() {
        selectTheCurrentSentence()

        val spans: Array<QuoteSpan> = text.getSpans(selectionStart, selectionEnd, QuoteSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            // removeTheQuotes()
            exists = true
        }

        if (!exists) {
            // wrapTheSentenceInQuotes()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                text.setSpan(QuoteSpan(AppearancePreferences.getAccentColor(), stripWidth, bulletGap),
                             selectionStart, selectionEnd,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                text.setSpan(QuoteSpan(AppearancePreferences.getAccentColor()),
                             selectionStart, selectionEnd,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            //            // Insert a newline at the right position
            //            if (rightSpace != text.length) {
            //                if (text[rightSpace + 1] != '\n') {
            //                    text.insert(rightSpace, "\n\n")
            //                }
            //            }
        }

        if (wasAlreadySelected.invert()) { // Remove the selection
            setSelection(cursorPosition, cursorPosition)
        }
    }

    fun EditText.blur() {
        val spans: Array<MaskFilterSpan> = text.getSpans(selectionStart, selectionEnd, MaskFilterSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            val blurMaskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.SOLID)
            text.setSpan(MaskFilterSpan(blurMaskFilter), selectionStart, selectionEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    fun EditText.alignCenter() {
        val spans: Array<AlignmentSpan> = text.getSpans(selectionStart, selectionEnd, AlignmentSpan::class.java)
        var exists = false

        for (span in spans) {
            text.removeSpan(span)
            exists = true
        }

        if (!exists) {
            text.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), selectionStart, selectionEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    // ************************************************************ //

    fun EditText.fixBrokenSpans(sequence: CharSequence): SpannableStringBuilder {
        val spans: Array<Objects> = text.getSpans(selectionStart, selectionEnd, Objects::class.java)
        val spannableStringBuilder = SpannableStringBuilder(sequence)

        for (span in spans) {
            text.removeSpan(span)
            spannableStringBuilder.setSpan(span, 0, sequence.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        return spannableStringBuilder
    }

    fun EditText.selectTheCurrentWord() {
        cursorPosition = selectionStart

        if (selectionStart != selectionEnd) {
            leftSpace = selectionStart
            rightSpace = selectionEnd
            wasAlreadySelected = true
            return
        } else {
            wasAlreadySelected = false
        }

        /**
         * Select the current word and check for the existence of a space, period and any special characters
         * including newline characters. If any of these characters exist then the selection stops before
         * the character.
         */
        leftSpace = selectionStart

        if (leftSpace > 0) {
            try {
                while (text[leftSpace - 1].isLetterOrDigit()) {
                    leftSpace--
                }
            } catch (_: IndexOutOfBoundsException) {
                /**
                 * We probably hit the beginning of the text
                 * so just ignore the exception.
                 */
            }
        }

        rightSpace = selectionEnd

        if (rightSpace < text.length) {
            try {
                while (text[rightSpace].isLetterOrDigit()) {
                    rightSpace++
                }
            } catch (_: IndexOutOfBoundsException) {
                /**
                 * We probably hit the end of the text
                 * so just ignore the exception.
                 */
            }
        }

        setSelection(leftSpace, rightSpace)
    }

    fun EditText.selectTheCurrentSentence() {
        cursorPosition = selectionStart

        if (selectionStart != selectionEnd) {
            leftSpace = selectionStart
            rightSpace = selectionEnd
            wasAlreadySelected = true
            return
        } else {
            wasAlreadySelected = false
        }

        /**
         * Find the first space on the left side of the cursor
         */
        leftSpace = selectionStart

        /**
         * If the cursor is not at the beginning of the text
         * and the character on the left side of the cursor is not a period
         * then find the first period on the left side of the cursor
         * and set the left space to the character after the period.
         */
        if (leftSpace > 0) {
            while (leftSpace > 0 && (text[leftSpace - 1].isLetterOrDigit() || text[leftSpace - 1] == ' ' || text[leftSpace - 1] == '"')) {
                leftSpace--
            }
        }

        /**
         * Find the first space on the right side of the cursor
         */
        rightSpace = selectionEnd

        /**
         * If the cursor is not at the end of the text
         * and the character on the right side of the cursor is not a period
         * then find the first period on the right side of the cursor
         * and set the right space to the character before the period.
         */
        if (rightSpace < text.length) {
            while (rightSpace < text.length && (text[rightSpace].isLetterOrDigit() || text[rightSpace] == ' ' || text[rightSpace] == '"')) {
                rightSpace++
            }
        }

        if (rightSpace != text.length) {
            rightSpace++
        }

        // Select the sentence
        setSelection(leftSpace, rightSpace)
    }

    fun EditText.wrapTheSentenceInQuotes() {
        /**
         * Add quote mark on the left and right side of the sentence
         */

        if (leftSpace == 0) {
            if (text[0] != '"') {
                text.insert(0, "\"")
                leftSpace--
            }
        } else {
            if (text[leftSpace - 1] != '"') {
                text.insert(leftSpace, "\"")
                leftSpace--
            }
        }

        if (rightSpace == text.length) {
            if (text[text.length - 1] != '"') {
                text.insert(text.length, "\"")
                rightSpace++
            }
        } else {
            if (text[rightSpace] != '"') {
                text.insert(rightSpace + 2, "\"")
                rightSpace += 3
            }
        }

        setSelection(leftSpace, rightSpace)
    }

    fun EditText.removeTheQuotes() {
        /**
         * Remove the quote marks on the left and right side of the sentence
         */

        if (leftSpace == 0) {
            if (text[0] == '"') {
                text.delete(0, 1)
                leftSpace++
            }
        } else {
            if (text[leftSpace - 1] == '"') {
                text.delete(leftSpace, leftSpace + 1)
                leftSpace++
            }
        }

        if (rightSpace == text.length) {
            if (text[text.length - 1] == '"') {
                text.delete(text.length - 1, text.length)
                rightSpace -= 3
            }
        } else {
            if (text[rightSpace] == '"') {
                text.delete(rightSpace - 1, rightSpace)
                rightSpace -= 3
            }
        }

        setSelection(leftSpace, rightSpace)
    }

    /**
     * Find all the occurrences of a search keyword in an EditText.
     *
     * Plan
     * 1. Convert the searchKeyword to lowercase.
     * 2. Initialize an empty list to store the match positions.
     * 3. Iterate through the text, extracting substrings of the same length as the searchKeyword.
     * 4. Compare each substring with the searchKeyword in a case-insensitive manner.
     * 5. If a match is found, add the start and end positions to the list.
     * 6. Return the list of match positions.
     */
    fun EditText.findMatches(searchKeyword: String): ArrayList<Pair<Int, Int>> {
        val lowerCaseKeyword = searchKeyword.lowercase()
        val list = ArrayList<Pair<Int, Int>>()
        val textLength = text.length
        val keywordLength = lowerCaseKeyword.length

        var index = 0
        while (index <= textLength - keywordLength) {
            val substring = text.subSequence(index, index + keywordLength).toString()
            if (substring.equals(lowerCaseKeyword, ignoreCase = true)) {
                list.add(Pair(index, index + keywordLength))
            }
            index++
        }

        return list
    }
}
