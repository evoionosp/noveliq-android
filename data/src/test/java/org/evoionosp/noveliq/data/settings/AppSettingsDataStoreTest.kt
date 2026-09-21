package org.evoionosp.noveliq.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.settings.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSettingsDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = AppSettingsDataStore(context)

    @After
    fun tearDown() =
        runTest {
            // The DataStore instance is shared through the app context, so reset
            // through the API rather than deleting the file underneath it.
            store.setThemePreference(AppSettings.DEFAULT_THEME_PREFERENCE)
            store.setDynamicColor(true)
            store.setSleepTimerMinutes(AppSettings.DEFAULT_SLEEP_TIMER_MINUTES)
        }

    @Test
    fun `defaults to system theme with dynamic color`() =
        runTest {
            store.settings.test {
                assertEquals(AppSettings(), awaitItem())
            }
        }

    @Test
    fun `theme preference updates are emitted`() =
        runTest {
            store.settings.test {
                awaitItem()

                store.setThemePreference("DARK")

                assertEquals("DARK", awaitItem().themePreference)
            }
        }

    @Test
    fun `dynamic color updates are emitted`() =
        runTest {
            store.settings.test {
                awaitItem()

                store.setDynamicColor(false)

                assertEquals(false, awaitItem().useDynamicColor)
            }
        }

    @Test
    fun `sleep timer minutes round-trip and clamp to the slider range`() =
        runTest {
            store.settings.test {
                assertEquals(AppSettings.DEFAULT_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)

                store.setSleepTimerMinutes(45)
                assertEquals(45, awaitItem().sleepTimerMinutes)

                store.setSleepTimerMinutes(180)
                assertEquals(AppSettings.MAX_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)

                store.setSleepTimerMinutes(0)
                assertEquals(AppSettings.MIN_SLEEP_TIMER_MINUTES, awaitItem().sleepTimerMinutes)
            }
        }
}
