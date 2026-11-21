package pulselab

import kotlin.math.sqrt

/** Core logic never invents measurements when samples are missing. */
data class Reading(val timestampMs: Long, val heartRate: Int?, val battery: Int?, val source: Source)
enum class Source { DEMO, DEVICE }
sealed class ParseResult {
    data class Valid(val bpm: Int): ParseResult()
    data class Invalid(val reason: String): ParseResult()
}
object SensorMath {
    fun heartRate(packet: ByteArray): ParseResult {
        if (packet.size < 2) return ParseResult.Invalid("Short packet")
        val is16 = packet[0].toInt() and 1 == 1
        if (is16 && packet.size < 3) return ParseResult.Invalid("Short 16-bit packet")
        val bpm = if (is16) (packet[1].toInt() and 255) or ((packet[2].toInt() and 255) shl 8)
                  else packet[1].toInt() and 255
        return if (bpm in 25..240) ParseResult.Valid(bpm) else ParseResult.Invalid("Out of range")
    }
    fun rmssd(rrMilliseconds: List<Double>): Double? {
        if (rrMilliseconds.size < 3 || rrMilliseconds.any { !it.isFinite() || it !in 250.0..2500.0 }) return null
        val squares = rrMilliseconds.zipWithNext { a,b -> (b-a)*(b-a) }
        return sqrt(squares.average())
    }
    fun meanRhr(bpm: List<Int>): Double? =
        if (bpm.isEmpty() || bpm.any { it !in 25..240 }) null else bpm.average()
    fun battery(percent: Int?): Int? = percent?.takeIf { it in 0..100 }
    fun safeDeviceName(name: String?): String =
        if (name?.contains("WHOOP", ignoreCase=true) == true) "WHOOP device" else "Unnamed BLE device"
    fun recovery(hrv: Double?, rhr: Double?, baselineHrv: Double?): Double? = null // Not validated
    fun demo(tick: Int): Reading = Reading(tick.toLong()*1000, 65+(tick%9), 80-(tick%5), Source.DEMO)
}
