package com.github.pedrovgs.deeppanel.app

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.github.pedrovgs.deeppanel.app.ExtractPanelsActivity.Companion.resource_id_extra
import com.karumi.shot.ScreenshotTest
import com.karumi.shot.waitForActivity
import org.junit.After
import org.junit.Test

class ExtractPanelsActivityTest : ScreenshotTest {

    private lateinit var scenario: ActivityScenario<ExtractPanelsActivity>

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun analyzesOneOfThePagesInformation() {
        val activity = startActivity(R.drawable.sample_page_10)

        compareExtractPanelsActivityScreenshot(activity)
    }

    private fun compareExtractPanelsActivityScreenshot(activity: ExtractPanelsActivity) =
        compareScreenshot(
            view = activity.findViewById(android.R.id.content),
            heightInPx = 1000
        )

    private fun startActivity(resource: Int): ExtractPanelsActivity {
        val intent = Intent(getInstrumentation().targetContext, ExtractPanelsActivity::class.java)
        intent.putExtra(resource_id_extra, resource)
        scenario = ActivityScenario.launch(intent)
        waitForAnimationsToFinish()
        return scenario.waitForActivity()
    }
}
