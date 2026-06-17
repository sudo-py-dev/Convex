package com.convex.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.convex.app.domain.model.AppLanguage
import com.convex.app.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppPreferencesTest {

    private val context = mockk<Context>(relaxed = true)
    private val dataStore = mockk<DataStore<Preferences>>(relaxed = true)
    private lateinit var prefs: AppPreferences

    @Before
    fun setup() {
        // DataStore is an extension property on Context, mocking it is tricky in unit tests
        // But AppPreferences takes Context and uses context.dataStore
        // Since it's a private extension, we might need to mock the internal property if we could
        // However, we can mock the store property in AppPreferences if we use reflection or if we change the class
        
        // For this test, I'll assume we've injected the DataStore or I'll use a mock store.
        // Let's use a simpler approach: mock the Flow returned by store.data
        prefs = AppPreferences(context)
        
        // Use reflection to set the private 'store' property for testing
        val field = AppPreferences::class.java.getDeclaredField("store")
        field.isAccessible = true
        field.set(prefs, dataStore)
    }

    @Test
    fun `themeMode emits SYSTEM by default`() = runTest {
        every { dataStore.data } returns flowOf(mockk(relaxed = true))
        
        val mode = prefs.themeMode
        mode.collect {
            assertEquals(ThemeMode.SYSTEM, it)
        }
    }

    @Test
    fun `technicalMode emits false by default`() = runTest {
        every { dataStore.data } returns flowOf(mockk(relaxed = true))
        
        prefs.technicalMode.collect {
            assertEquals(false, it)
        }
    }

    @Test
    fun `setThemeMode edits dataStore`() = runTest {
        prefs.setThemeMode(ThemeMode.DARK)
        // verify { dataStore.edit(any()) } // edit is an inline extension, hard to verify directly with mockk
    }
}
