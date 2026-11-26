import CoreHaptics
import UIKit

/// Haptic feedback with UIKit feedback generators, and custom vibrations and patterns with Core Haptics.
final class HapticsPlugin: WefterPlugin {

    private var hapticEngine: CHHapticEngine?
    private var activePlayer: CHHapticPatternPlayer?

    /// Whether the device supports Core Haptics. Intensity can always be controlled when it does.
    // @WefterMethod
    func isAvailable(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let supportsHaptics = CHHapticEngine.capabilitiesForHardware().supportsHaptics
        resolve(callback, data: [
            "available": supportsHaptics,
            "amplitudeControlSupported": supportsHaptics,
        ])
    }

    /// Plays an impact of the given style, at `intensity` when given.
    // @WefterMethod
    func impact(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let styleName = payload["style"] as? String ?? "medium"
        guard let style = HapticsPlugin.impactStyle(for: styleName) else {
            reject(callback, code: "INVALID_STYLE", message: "Unknown impact style: \(styleName). Valid styles are: light, soft, medium, rigid, heavy.")
            return
        }

        let generator = UIImpactFeedbackGenerator(style: style)
        generator.prepare()
        if let intensity = (payload["intensity"] as? NSNumber)?.doubleValue {
            generator.impactOccurred(intensity: CGFloat(min(max(intensity, 0), 1)))
        } else {
            generator.impactOccurred()
        }
        resolve(callback, data: ["played": true])
    }

    /// Plays success, warning or error feedback.
    // @WefterMethod
    func notification(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let typeName = payload["type"] as? String ?? ""
        guard let type = HapticsPlugin.notificationType(for: typeName) else {
            reject(callback, code: "INVALID_TYPE", message: "Unknown notification type: \(typeName). Valid types are: success, warning, error.")
            return
        }

        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(type)
        resolve(callback, data: ["played": true])
    }

    /// Plays a selection tick.
    // @WefterMethod
    func selection(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let generator = UISelectionFeedbackGenerator()
        generator.prepare()
        generator.selectionChanged()
        resolve(callback, data: ["played": true])
    }

    /// Plays a continuous vibration of `duration` milliseconds, or a `pattern` of alternating wait and
    /// vibrate
    /// durations, optionally repeating.
    // @WefterMethod
    func vibrate(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        let rawAmplitude = (payload["amplitude"] as? NSNumber)?.doubleValue ?? 1.0
        let amplitude = Float(min(max(rawAmplitude, 0), 1))

        if let patternArray = payload["pattern"] as? [NSNumber] {
            guard !patternArray.isEmpty else {
                reject(callback, code: "INVALID_PATTERN", message: "pattern must not be empty")
                return
            }

            let timingsMs = patternArray.map { $0.doubleValue }
            guard timingsMs.allSatisfy({ $0 >= 0 }) else {
                reject(callback, code: "INVALID_PATTERN", message: "pattern durations must not be negative")
                return
            }

            let repeatFlag = payload["repeat"] as? Bool ?? false
            playPattern(timingsSeconds: timingsMs.map { $0 / 1000.0 }, amplitude: amplitude, repeats: repeatFlag, callback: callback)
            return
        }

        let durationMs = (payload["duration"] as? NSNumber)?.doubleValue ?? 200
        guard durationMs > 0 else {
            reject(callback, code: "INVALID_DURATION", message: "duration must be greater than 0")
            return
        }
        playPattern(timingsSeconds: [durationMs / 1000.0], amplitude: amplitude, repeats: false, callback: callback)
    }

    /// Stops any vibration in progress.
    // @WefterMethod
    func cancel(payload: [String: Any], callback: @escaping (Result<Any, Error>) -> Void) throws {
        if let player = activePlayer {
            try? player.stop(atTime: CHHapticTimeImmediate)
            activePlayer = nil
        }
        resolve(callback, data: ["cancelled": true])
    }

    /// Plays a pattern on the haptic engine. Even entries are pauses and odd entries vibrations, in seconds.
    private func playPattern(timingsSeconds: [Double], amplitude: Float, repeats: Bool, callback: @escaping (Result<Any, Error>) -> Void) {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else {
            reject(callback, code: "NOT_AVAILABLE", message: "This device does not support custom haptic patterns")
            return
        }

        do {
            let engine = try activeEngine()

            var events: [CHHapticEvent] = []
            var time: Double = 0
            for (index, segment) in timingsSeconds.enumerated() {
                if index % 2 == 0 {
                    let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: amplitude)
                    let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.5)
                    events.append(CHHapticEvent(eventType: .hapticContinuous, parameters: [intensity, sharpness], relativeTime: time, duration: segment))
                }
                time += segment
            }

            guard !events.isEmpty else {
                reject(callback, code: "INVALID_PATTERN", message: "pattern must include at least one on-segment")
                return
            }

            let pattern = try CHHapticPattern(events: events, parameterCurves: [])

            if repeats {
                let player = try engine.makeAdvancedPlayer(with: pattern)
                player.loopEnabled = true
                try player.start(atTime: CHHapticTimeImmediate)
                activePlayer = player
            } else {
                let player = try engine.makePlayer(with: pattern)
                try player.start(atTime: CHHapticTimeImmediate)
                activePlayer = player
            }

            resolve(callback, data: ["played": true])
        } catch {
            reject(callback, code: "VIBRATE_FAILED", message: "Could not trigger vibration: \(error.localizedDescription)")
        }
    }

    /// The Core Haptics engine, started on first use and restarted whenever the system stops or resets it.
    private func activeEngine() throws -> CHHapticEngine {
        if let engine = hapticEngine {
            return engine
        }

        let engine = try CHHapticEngine()
        engine.stoppedHandler = { [weak engine] _ in
            try? engine?.start()
        }
        engine.resetHandler = { [weak engine] in
            try? engine?.start()
        }
        try engine.start()
        hapticEngine = engine
        return engine
    }

    /// UIKit impact style for a style name, or `nil` when unknown.
    private static func impactStyle(for name: String) -> UIImpactFeedbackGenerator.FeedbackStyle? {
        switch name {
        case "light": return .light
        case "soft": return .soft
        case "medium": return .medium
        case "rigid": return .rigid
        case "heavy": return .heavy
        default: return nil
        }
    }

    /// UIKit notification type for a type name, or `nil` when unknown.
    private static func notificationType(for name: String) -> UINotificationFeedbackGenerator.FeedbackType? {
        switch name {
        case "success": return .success
        case "warning": return .warning
        case "error": return .error
        default: return nil
        }
    }
}
