package ir.memberexpiry.manager.core.date

import java.util.Locale

object PersianText {
    fun toAsciiDigits(value: String): String = value.map {
        when (it) {
            in '۰'..'۹' -> '0' + (it - '۰')
            in '٠'..'٩' -> '0' + (it - '٠')
            else -> it
        }
    }.joinToString("")

    fun toPersianDigits(value: String): String = toAsciiDigits(value).map {
        if (it in '0'..'9') '۰' + (it - '0') else it
    }.joinToString("")

    fun normalizeSearch(value: String): String = toAsciiDigits(value)
        .replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace('\u200C', ' ').replace('\u200D', ' ')
        .replace(Regex("[\\s\\u00A0]+"), " ").trim().lowercase(Locale.ROOT)

    fun nationalCode(value: String): String = toAsciiDigits(value).trim().also {
        require(it.matches(Regex("[0-9]{10}"))) { "National code must contain exactly 10 digits" }
    }
}
