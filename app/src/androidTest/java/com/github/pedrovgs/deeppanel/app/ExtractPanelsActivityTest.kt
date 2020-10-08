package com.github.pedrovgs.deeppanel.app

import android.content.Intent
import android.view.View
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
    fun analyzesPagesInformationForPage0() {
        val activity = startActivity(R.drawable.sample_page_0)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage1() {
        val activity = startActivity(R.drawable.sample_page_1)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage2() {
        val activity = startActivity(R.drawable.sample_page_2)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage3() {
        val activity = startActivity(R.drawable.sample_page_3)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage4() {
        val activity = startActivity(R.drawable.sample_page_4)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage5() {
        val activity = startActivity(R.drawable.sample_page_5)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage6() {
        val activity = startActivity(R.drawable.sample_page_6)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage7() {
        val activity = startActivity(R.drawable.sample_page_7)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage8() {
        val activity = startActivity(R.drawable.sample_page_8)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage9() {
        val activity = startActivity(R.drawable.sample_page_9)

        compareExtractPanelsActivityScreenshot(activity)
    }

    @Test
    fun analyzesPagesInformationForPage10() {
        val activity = startActivity(R.drawable.sample_page_10)

        compareExtractPanelsActivityScreenshot(activity)
    }

    private fun compareExtractPanelsActivityScreenshot(activity: ExtractPanelsActivity) {
        val root = activity.findViewById<View>(android.R.id.content)
        runOnUi {
            root.setBackgroundResource(android.R.color.white)
        }
        return compareScreenshot(
            view = root,
            heightInPx = 2000
        )
    }

    private fun startActivity(resource: Int): ExtractPanelsActivity {
        val intent = Intent(getInstrumentation().targetContext, ExtractPanelsActivity::class.java)
        intent.putExtra(resource_id_extra, resource)
        scenario = ActivityScenario.launch(intent)
        waitForIdle()
        return scenario.waitForActivity()
    }

    private fun waitForIdle() {
        waitForAnimationsToFinish()
    }
}
