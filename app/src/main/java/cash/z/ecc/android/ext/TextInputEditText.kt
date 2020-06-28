package cash.z.ecc.android.ext

import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import cash.z.ecc.android.R
import com.google.android.material.textfield.TextInputEditText

inline fun TextInputEditText.limitDecimalPlaces(max: Int) {
    this.apply {
        var previousValue = ""
        doBeforeTextChanged { text: CharSequence?, _: Int, _: Int, _: Int ->
            previousValue = text.toString()
        }
        doAfterTextChanged {
            var textStr = text.toString()
            if (textStr.contains('.') || textStr.contains(',')) {
                if (textStr.first() == '.' || textStr.first() == ',') {
                    textStr = "0$textStr"
                }
                if (textStr.last() != '.' && textStr.last() != ',') {
                    val dotIndex = textStr.indexOfFirst { c -> c == '.' || c == ',' }
                    if (textStr.length - dotIndex > max + 1) {
                        var oneChange = previousValue.length == textStr.length - 1
                        if (oneChange) {
                            var offset = 0
                            for (i in 0 until textStr.length - 1) {
                                if (textStr[i + offset] != previousValue[i]) {
                                    if (offset == 0) {
                                        offset = 1
                                    } else {
                                        oneChange = false
                                        break
                                    }
                                }
                            }
                        }
                        if (oneChange) {
                            textStr = previousValue
                        } else {
                            textStr = textStr.removeRange(dotIndex + max + 1, textStr.length)
                        }
                    }
                }
            }

            val oldText = this.text.toString()

            for (i in 1 until textStr.length) {
                if (textStr[i-1] != '0') {
                    break
                } else if (textStr[i] != '.') {
                    textStr = textStr.removeRange(0, i)
                }
            }

            if (oldText != textStr) {
                val cursorPosition = this.selectionEnd;
                this.setText(textStr)
                this.setSelection(
                    (cursorPosition - (oldText.length - textStr.length)).coerceIn(0, this.text.toString().length)
                )
            }
        }
    }
}