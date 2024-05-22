package com.`sioptik-test`.main

import android.app.Activity
import android.app.Instrumentation
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sioptik.main.Kamera
import com.sioptik.main.MainActivity
import com.sioptik.main.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HalamanUtamaTest {
    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>

    @get:Rule
    val intentsRule = IntentsRule()

    @Before
    fun setup() {
        mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        mainActivityScenario.moveToState(Lifecycle.State.RESUMED)
    }

    /**
     * Tests if MainActivity triggers an intent to start Kamera activity when
     * scan_button is clicked
     */
    @Test
    fun testScanButtonClick() {
        intending(hasComponent(Kamera::class.java.name)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        onView(withId(R.id.scan_button)).perform(click())
        intended(hasComponent(Kamera::class.java.name))
    }
}