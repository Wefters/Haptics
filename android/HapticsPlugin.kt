package dev.wefter.bridge

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.json.JSONObject

class HapticsPlugin(context: Context, dispatcher: BridgeDispatcher) :
        WefterPlugin(context, dispatcher) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @WefterMethod
    fun isAvailable(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        val hasVibrator = vibrator.hasVibrator()
        val amplitudeControlSupported =
                hasVibrator &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        vibrator.hasAmplitudeControl()
        resolve(
                callback,
                JSONObject()
                        .put("available", hasVibrator)
                        .put("amplitudeControlSupported", amplitudeControlSupported)
        )
    }

    @WefterMethod
    fun impact(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        val style = payload.optString("style", "medium")
        val preset = IMPACT_PRESETS[style]
        if (preset == null) {
            reject(
                    callback,
                    "INVALID_STYLE",
                    "Unknown impact style: $style. Valid styles are: ${IMPACT_PRESETS.keys.joinToString(", ")}."
            )
            return
        }

        val intensity =
                if (payload.has("intensity") && !payload.isNull("intensity")) {
                    payload.optDouble("intensity")
                } else {
                    null
                }
        val amplitude = intensity?.let { toAmplitude(it) } ?: preset.amplitude

        val predefinedEffect =
                if (intensity == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    when (style) {
                        "light" -> VibrationEffect.EFFECT_TICK
                        "medium" -> VibrationEffect.EFFECT_CLICK
                        "heavy" -> VibrationEffect.EFFECT_HEAVY_CLICK
                        else -> null
                    }
                } else {
                    null
                }

        triggerVibration(
                callback,
                duration = preset.durationMs,
                amplitude = amplitude,
                predefinedEffect = predefinedEffect
        )
    }

    @WefterMethod
    fun notification(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        when (payload.optString("type", "")) {
            "success" ->
                    triggerVibration(
                            callback,
                            pattern = longArrayOf(0, 40, 60, 40),
                            amplitude = 180
                    )
            "warning" -> triggerVibration(callback, duration = 60, amplitude = 200)
            "error" ->
                    triggerVibration(
                            callback,
                            pattern = longArrayOf(0, 50, 80, 50, 80, 50),
                            amplitude = 255
                    )
            else -> {
                val type = payload.optString("type", "")
                reject(
                        callback,
                        "INVALID_TYPE",
                        "Unknown notification type: $type. Valid types are: success, warning, error."
                )
            }
        }
    }

    @WefterMethod
    fun selection(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        val predefinedEffect =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK
                else null
        triggerVibration(
                callback,
                duration = 8,
                amplitude = 80,
                predefinedEffect = predefinedEffect
        )
    }

    @WefterMethod
    fun vibrate(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        val amplitude =
                if (payload.has("amplitude") && !payload.isNull("amplitude")) {
                    toAmplitude(payload.optDouble("amplitude"))
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }

        val patternArray = payload.optJSONArray("pattern")
        if (patternArray != null) {
            if (patternArray.length() == 0) {
                reject(callback, "INVALID_PATTERN", "pattern must not be empty")
                return
            }

            val jsTimings = LongArray(patternArray.length()) { i -> patternArray.optLong(i, 0) }
            if (jsTimings.any { it < 0 }) {
                reject(callback, "INVALID_PATTERN", "pattern durations must not be negative")
                return
            }

            val androidTimings = LongArray(jsTimings.size + 1)
            jsTimings.copyInto(androidTimings, destinationOffset = 1)

            val repeatFlag = payload.optBoolean("repeat", false)
            triggerVibration(
                    callback,
                    pattern = androidTimings,
                    amplitude = amplitude,
                    repeat = repeatFlag
            )
            return
        }

        val duration = payload.optLong("duration", 200L)
        if (duration <= 0) {
            reject(callback, "INVALID_DURATION", "duration must be greater than 0")
            return
        }
        triggerVibration(callback, duration = duration, amplitude = amplitude)
    }

    @WefterMethod
    fun cancel(payload: JSONObject, callback: (Result<Any>) -> Unit) {
        vibrator.cancel()
        resolve(callback, JSONObject().put("cancelled", true))
    }

    private fun triggerVibration(
            callback: (Result<Any>) -> Unit,
            duration: Long? = null,
            pattern: LongArray? = null,
            amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE,
            repeat: Boolean = false,
            predefinedEffect: Int? = null,
    ) {
        if (!vibrator.hasVibrator()) {
            reject(callback, "NOT_AVAILABLE", "This device has no vibrator")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect =
                        when {
                            predefinedEffect != null &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                                    VibrationEffect.createPredefined(predefinedEffect)
                            pattern != null ->
                                    VibrationEffect.createWaveform(
                                            pattern,
                                            IntArray(pattern.size) { i ->
                                                if (i % 2 == 0) 0 else amplitude
                                            },
                                            if (repeat) 0 else -1
                                    )
                            else ->
                                    VibrationEffect.createOneShot(
                                            (duration ?: 200L).coerceAtLeast(1),
                                            amplitude
                                    )
                        }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                if (pattern != null) {
                    vibrator.vibrate(pattern, if (repeat) 0 else -1)
                } else {
                    vibrator.vibrate((duration ?: 200L).coerceAtLeast(1))
                }
            }
            resolve(callback, JSONObject().put("played", true))
        } catch (e: IllegalArgumentException) {
            reject(callback, "INVALID_PATTERN", e.message ?: "Invalid vibration pattern")
        } catch (e: Exception) {
            reject(callback, "VIBRATE_FAILED", e.message ?: "Could not trigger vibration")
        }
    }

    private fun toAmplitude(intensity: Double): Int =
            (intensity.coerceIn(0.0, 1.0) * 254 + 1).toInt().coerceIn(1, 255)

    private data class ImpactPreset(val durationMs: Long, val amplitude: Int)

    companion object {
        private val IMPACT_PRESETS =
                mapOf(
                        "light" to ImpactPreset(10L, 90),
                        "soft" to ImpactPreset(15L, 110),
                        "medium" to ImpactPreset(20L, 150),
                        "rigid" to ImpactPreset(15L, 200),
                        "heavy" to ImpactPreset(30L, 255),
                )
    }
}
