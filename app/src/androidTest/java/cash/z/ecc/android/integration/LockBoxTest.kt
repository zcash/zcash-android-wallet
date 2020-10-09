package cash.z.ecc.android.integration

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.lockbox.LockBox
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LockBoxTest {

    lateinit var lockBox: LockBox
    lateinit var appContext: Context
    private val hex = ('a'..'f') + ('0'..'9')
    private val iterations = 50

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        lockBox = LockBox(appContext)
        lockBox.clear()
    }

    @Test
    fun testLongString() {
        var successCount = 0
        repeat(iterations) {
            val sampleHex = List(500) { hex.random() }.joinToString("")

            lockBox["longStr"] = sampleHex
            val actual: String = lockBox["longStr"]!!
            if(sampleHex == actual) successCount++
            lockBox.clear()
        }
        assertEquals(iterations, successCount)
    }

    @Test
    fun testShortString() {
        var successCount = 0
        repeat(iterations) {
            val sampleHex = List(50) { hex.random() }.joinToString("")

            lockBox["shortStr"] = sampleHex
            val actual: String = lockBox["shortStr"]!!
            if(sampleHex == actual) successCount++
            lockBox.clear()
        }
        assertEquals(iterations, successCount)
    }

    @Test
    fun testGiantString() {
        var successCount = 0
        repeat(iterations) {
            val sampleHex = List(2500) { hex.random() }.joinToString("")

            lockBox["giantStr"] = sampleHex
            val actual: String = lockBox["giantStr"]!!
            if(sampleHex == actual) successCount++
            lockBox.clear()
        }
        assertEquals(iterations, successCount)
    }
}
