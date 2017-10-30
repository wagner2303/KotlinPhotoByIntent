package com.wagner2303.kotlinphotobyintent

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.idling.CountingIdlingResource
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.xamarin.testcloud.espresso.Factory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var reportHelper = Factory.getReportHelper()

    @Before
    fun setup() {
        val idlingResource = CountingIdlingResource("MainActivity", true)
        mActivityRule.activity.idleResource = idlingResource
        Espresso.registerIdlingResources(idlingResource)
    }

    @Test
    @Throws(Exception::class)
    fun testResizeMultipleImages() {
        val testFiles = listOf("04", "05", "06", "08", "09", "12")

        for (file in testFiles) {
            onView(withText(file)).perform(click())
            reportHelper.label("$file MB image")
        }
    }

    @After
    fun afterTest() {
        reportHelper.label("Finished uploading")
    }
}
