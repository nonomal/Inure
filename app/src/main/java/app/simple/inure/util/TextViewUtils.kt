package app.simple.inure.util

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.text.Html
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import app.simple.inure.interfaces.parsers.LinkCallbacks
import app.simple.inure.util.StringUtils.fetchLinks

object TextViewUtils {
    fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    /**
                     * use this to change the link color
                     */
                    textPaint.color = Color.parseColor("#2e86c1")

                    /**
                     * Toggle below value to enable/disable
                     * the underline shown below the clickable text
                     */
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    link.second.onClick(view)
                }
            }
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
            // if(startIndexOfLink == -1) continue // if you want to verify your texts contains links text
            spannableString.setSpan(clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        this.movementMethod = LinkMovementMethod.getInstance()  // without LinkMovementMethod, links can not be clicked
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    fun TextView.makeLinksClickable(string: String, linkCallbacks: LinkCallbacks) {
        val links = string.fetchLinks()
        val spannableString = SpannableString(string)
        var startIndexOfLink = -1

        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    /**
                     * use this to change the link color
                     */
                    textPaint.color = Color.parseColor("#2e86c1")

                    /**
                     * Toggle below value to enable/disable
                     * the underline shown below the clickable text
                     */
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    linkCallbacks.onLinkClicked(link, view)
                }
            }

            startIndexOfLink = string.indexOf(link, startIndexOfLink + 1)
            // if(startIndexOfLink == -1) continue // if you want to verify your texts contains links text
            spannableString.setSpan(clickableSpan, startIndexOfLink, startIndexOfLink + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (links.isNotEmpty()) {
            this.movementMethod = LinkMovementMethod.getInstance() // without LinkMovementMethod, links can not be clicked
            this.setText(spannableString, TextView.BufferType.SPANNABLE)
        } else {
            this.text = string
        }
    }

    fun TextView.makeLinksClickable(spanned: Spanned, linkCallbacks: LinkCallbacks) {
        val links = spanned.toString().fetchLinks()
        val spannableString = SpannableString(spanned)
        var startIndexOfLink = -1

        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    /**
                     * use this to change the link color
                     */
                    textPaint.color = Color.parseColor("#2e86c1")

                    /**
                     * Toggle below value to enable/disable
                     * the underline shown below the clickable text
                     */
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    linkCallbacks.onLinkClicked(link, view)
                }
            }

            startIndexOfLink = spanned.toString().indexOf(link, startIndexOfLink + 1)
            // if(startIndexOfLink == -1) continue // if you want to verify your texts contains links text
            spannableString.setSpan(clickableSpan, startIndexOfLink, startIndexOfLink + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (links.isNotEmpty()) {
            this.movementMethod = LinkMovementMethod.getInstance() // without LinkMovementMethod, links can not be clicked
            this.setText(spannableString, TextView.BufferType.SPANNABLE)
        } else {
            this.text = spanned
        }
    }

    fun TextView.makeClickable(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    /**
                     * use this to change the link color
                     */
                    textPaint.color = this@makeClickable.currentTextColor

                    /**
                     * Toggle below value to enable/disable
                     * the underline shown below the clickable text
                     */
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    link.second.onClick(view)
                }
            }
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
            // if(startIndexOfLink == -1) continue // if you want to verify your texts contains links text
            spannableString.setSpan(
                    clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.movementMethod = LinkMovementMethod.getInstance()  // without LinkMovementMethod, links can not be clicked
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    fun String.toHtmlSpanned(): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this)
        }
    }

    fun TextView.setDrawableTint(color: Int) {
        for (drawable in this.compoundDrawablesRelative) {
            drawable?.mutate()
            drawable?.colorFilter = PorterDuffColorFilter(
                    color, PorterDuff.Mode.SRC_IN
            )
        }
    }

    fun TextView.setDrawableLeft(drawableLeft: Int, color: Int = -1) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(drawableLeft, 0, 0, 0)
        if (color != -1) {
            setDrawableTint(color)
        }
    }

    fun TextView.setDrawableRight(drawableRight: Int, color: Int = -1) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawableRight, 0)
        if (color != -1) {
            setDrawableTint(color)
        }
    }

    fun AppCompatEditText.setDrawableTint(color: Int) {
        for (drawable in this.compoundDrawablesRelative) {
            drawable?.mutate()
            drawable?.colorFilter = PorterDuffColorFilter(
                    color, PorterDuff.Mode.SRC_IN
            )
        }
    }

    inline fun TextView.doOnTextChanged(
            crossinline action: (
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
            ) -> Unit
    ): TextWatcher = addTextChangedListener(onTextChanged = action)
}
