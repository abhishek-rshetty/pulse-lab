package pulselab

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReadingsTest {
    @Test fun emptyPacket() = assertTrue(SensorMath.heartRate(byteArrayOf()) is ParseResult.Invalid)
    @Test fun oneBytePacket() = assertTrue(SensorMath.heartRate(byteArrayOf(0)) is ParseResult.Invalid)
    @Test fun eightBitHeartRate() = assertEquals(ParseResult.Valid(72), SensorMath.heartRate(byteArrayOf(0,72)))
    @Test fun sixteenBitHeartRate() = assertEquals(ParseResult.Valid(180), SensorMath.heartRate(byteArrayOf(1,180.toByte(),0)))
    @Test fun truncatedSixteenBit() = assertTrue(SensorMath.heartRate(byteArrayOf(1,80)) is ParseResult.Invalid)
    @Test fun invalidZero() = assertTrue(SensorMath.heartRate(byteArrayOf(0,0)) is ParseResult.Invalid)
    @Test fun invalidHighHeartRate() = assertTrue(SensorMath.heartRate(byteArrayOf(1,255.toByte(),0)) is ParseResult.Invalid)
    @Test fun validHighHeartRate() = assertEquals(ParseResult.Valid(240), SensorMath.heartRate(byteArrayOf(0,240.toByte())))
    @Test fun missingRr() = assertNull(SensorMath.rmssd(emptyList()))
    @Test fun tooFewRr() = assertNull(SensorMath.rmssd(listOf(800.0,810.0)))
    @Test fun invalidRr() = assertNull(SensorMath.rmssd(listOf(800.0,100.0,850.0)))
    @Test fun nonFiniteRr() = assertNull(SensorMath.rmssd(listOf(800.0,Double.NaN,810.0)))
    @Test fun constantRrRmssd() = assertEquals(0.0,SensorMath.rmssd(listOf(800.0,800.0,800.0))!!,0.0001)
    @Test fun changingRrRmssd() = assertEquals(10.0,SensorMath.rmssd(listOf(800.0,810.0,800.0))!!,0.0001)
    @Test fun emptyRhr() = assertNull(SensorMath.meanRhr(emptyList()))
    @Test fun badRhr() = assertNull(SensorMath.meanRhr(listOf(50,999)))
    @Test fun averageRhr() = assertEquals(55.0,SensorMath.meanRhr(listOf(50,60))!!,0.0001)
    @Test fun batteryBounds() { assertNull(SensorMath.battery(-1)); assertEquals(0,SensorMath.battery(0)); assertEquals(100,SensorMath.battery(100)); assertNull(SensorMath.battery(101)) }
    @Test fun recoveryNeverInvented() = assertNull(SensorMath.recovery(100.0,55.0,90.0))
    @Test fun demoIsLabeled() = assertEquals(Source.DEMO,SensorMath.demo(4).source)
    @Test fun deviceNameHidden() = assertEquals("WHOOP device",SensorMath.safeDeviceName("WHOOP 5.0 AB:CD"))
}
