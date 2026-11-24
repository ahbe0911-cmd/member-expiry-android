package ir.memberexpiry.manager.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.ui.theme.MemberExpiryTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MemberFormTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalidInputDoesNotSubmit() {
        var submitted = false
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MemberExpiryTheme {
                    MemberForm(null, JalaliDate(1405, 9, 3), false, null, {}, { submitted = true })
                }
            }
        }
        compose.onNodeWithText("ثبت عضویت").performClick()
        compose.onNodeWithText("افزودن عضو جدید").assertIsDisplayed()
        compose.runOnIdle { assertFalse(submitted) }
    }
}
