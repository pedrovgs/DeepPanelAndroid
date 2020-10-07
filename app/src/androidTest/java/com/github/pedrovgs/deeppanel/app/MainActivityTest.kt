package com.github.pedrovgs.deeppanel.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.karumi.shot.ScreenshotTest
import com.karumi.shot.waitForActivity
import org.junit.After
import org.junit.Test

class MainActivityTest : ScreenshotTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun analyzesEveryPageImageAndGeneratesThePanelsOverlays() {
        val activity = startActivity()

        Pages.resList.forEachIndexed { index, _ ->
            compareScreenshot(activity = activity, name = "Panels analysis for page $index")

            clickOnGoToNextPage()
        }
    }

    private fun clickOnGoToNextPage() {
        onView(withId(R.id.nextPageButton)).perform(click())
        waitForIdle()
    }

    private fun startActivity(): MainActivity {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val activity = scenario.waitForActivity()
        waitForIdle()
        return activity
    }

    private fun waitForIdle() {
        waitForAnimationsToFinish()
    }
}
