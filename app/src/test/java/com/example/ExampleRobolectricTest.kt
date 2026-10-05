package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RotinaUp", appName)
  }

  @Test
  fun `test phrase normalizer matches with diacritics and case`() {
    val target = "Estou no comando do meu tempo"
    val typed = "  éstou no comando DO meu tempo!  "
    val matches = com.example.util.TextNormalizer.isPhraseMatch(typed, target)
    assertEquals(true, matches)
  }

  @Test
  fun `test launch MainActivity`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }
}
