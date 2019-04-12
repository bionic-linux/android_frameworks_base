package android.net.metrics

import android.net.metrics.DhcpErrorEvent.errorCodeWithOption
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.android.internal.util.TestUtils.parcelingRoundTrip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Modifier

private const val TEST_ERROR_CODE = 12345

@RunWith(AndroidJUnit4::class)
@SmallTest
class DhcpErrorEventTest {

    @Test
    fun testConstructor() {
        val event = DhcpErrorEvent(TEST_ERROR_CODE)
        assertEquals(TEST_ERROR_CODE, event.errorCode)
    }

    @Test
    fun testParcelUnparcel() {
        val event = DhcpErrorEvent(TEST_ERROR_CODE)
        val parceled = parcelingRoundTrip(event)
        assertEquals(TEST_ERROR_CODE, parceled.errorCode)
    }

    @Test
    fun testErrorCodeWithOption() {
        assertEquals(0, errorCodeWithOption(0, 0))
        assertEquals(1, errorCodeWithOption(0xFFFF, 1))
        assertEquals(0x80007, errorCodeWithOption(0x8FFFF, 0xFFFF0007.toInt()))
        assertEquals(0x12340021, errorCodeWithOption(0x12345678, 0x87654321.toInt()))
    }

    @Test
    fun testToString() {
        val errorFields = DhcpErrorEvent::class.java.declaredFields.filter {
            it.type == Int::class.javaPrimitiveType
                    && Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers)
                    && it.name !in listOf(
                    "L2_ERROR", "L3_ERROR", "L4_ERROR", "DHCP_ERROR", "MISC_ERROR")
        }

        errorFields.forEach {
            val intValue = it.get(null) as Int
            val stringValue = DhcpErrorEvent(intValue).toString()
            assertTrue(String.format(
                    "Invalid string for error 0x%08X (field ${it.name}): $stringValue", intValue),
                    stringValue.contains(it.name))
        }
    }

    @Test
    fun testToString_InvalidErrorCode() {
        assertNotNull(DhcpErrorEvent(TEST_ERROR_CODE).toString())
    }
}