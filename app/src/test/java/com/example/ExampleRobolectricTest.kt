package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesRepository
import com.example.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readAppNameAndValidateGithubRepoParsing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hardik Buds Pro", appName)

        val repo = PreferencesRepository(context)
        val updater = AppUpdateManager(context, repo)
        val parsed = updater.parseOwnerRepo("https://github.com/hardik-audio/hardik-buds-pro-ultra-max")
        assertNotNull(parsed)
        assertEquals("hardik-audio", parsed?.first)
        assertEquals("hardik-buds-pro-ultra-max", parsed?.second)
        assertNull(updater.parseOwnerRepo("http://insecure.example.com/owner/repo"))
    }
}
