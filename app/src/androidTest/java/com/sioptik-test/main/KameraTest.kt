import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.sioptik.main.Kamera
import com.sioptik.main.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class KameraTest {

    @Test
    fun captureButton_isClickable() {
        ActivityScenario.launch(Kamera::class.java)

        onView(withId(R.id.captureButton))
            .check(matches(isClickable()))

        onView(withId(R.id.captureButton)).perform(click())
    }

}
