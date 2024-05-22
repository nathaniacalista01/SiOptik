import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.sioptik.main.R
import com.sioptik.main.SplashScreen
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith

fun withDrawable(resourceId: Int): Matcher<View> {
    return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("with drawable from resource id: ")
            description?.appendValue(resourceId)
        }

        override fun matchesSafely(imageView: ImageView?): Boolean {
            val context = imageView?.context
            val expectedDrawable: Drawable? = context?.getDrawable(resourceId)
            val actualDrawable: Drawable? = imageView?.drawable
            return expectedDrawable == actualDrawable
        }
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
class SplashScreenTest {

    @Test
    fun assureDisplayed() {
        ActivityScenario.launch(SplashScreen::class.java)

        onView(withId(R.id.textView))
            .check(matches(withText("SiOptik")))

        onView(withId(R.id.SplashScreen))
            .check(matches(isDisplayed()))
            .check(matches(withDrawable(R.drawable.gajah_kpu)))
    }

}
