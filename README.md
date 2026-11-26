# @wefterjs/haptics

Official Wefter plugin for physical device haptic and vibration feedback on Android and iOS.

## Features

- Check hardware presence and amplitude control support.
- Trigger physical impact feedback with multiple styles (light, soft, medium, rigid, heavy).
- Play standard notification haptic patterns (success, warning, error).
- Trigger selection feedback for UI controls like wheels, pickers, and sliders.
- Execute custom vibration patterns with timing and amplitude control.
- Cancel active vibrations programmatically.

## Installation and setup

Install the plugin package in your Wefter application:

```bash
wefter add @wefterjs/haptics
wefter sync
```

### Native permissions

When synchronized:

- Android automatically requests `<uses-permission android:name="android.permission.VIBRATE" />` in `AndroidManifest.xml`.
- iOS requires no additional permissions.

## JavaScript API reference

Import `Haptics` from `@wefterjs/haptics`:

```ts
import { Haptics } from "@wefterjs/haptics";
```

### Check hardware support

```ts
const status = await Haptics.isAvailable();
console.log("Haptics available:", status.available);
console.log("Amplitude control:", status.amplitudeControlSupported);
```

### Impact feedback

Trigger physical impact feedback to accompany collisions, button presses, or gestures:

```ts
// Styles: "light", "soft", "medium", "rigid", or "heavy"
await Haptics.impact({
  style: "medium",
  intensity: 0.8, // Optional normalized intensity between 0.0 and 1.0
});
```

### Notification patterns

Play feedback sequences indicating operation results:

```ts
// Types: "success", "warning", or "error"
await Haptics.notification({ type: "success" });
```

### Selection feedback

Provide subtle tactile feedback when the user moves through items in a list, date picker, or carousel:

```ts
await Haptics.selection();
```

### Custom vibration patterns

Play custom duration vibrations or timed pattern sequences:

```ts
// Single vibration for 300 milliseconds
await Haptics.vibrate({ duration: 300 });

// Pattern sequence with timing (alternating wait and vibrate ms)
await Haptics.vibrate({
  pattern: [0, 100, 50, 100], // wait 0ms, vibrate 100ms, wait 50ms, vibrate 100ms
  amplitude: 128, // 1 to 255 (Android only, ignored on devices without amplitude control)
  repeat: false,
});
```

### Cancel ongoing vibration

Stop ongoing pattern vibrations immediately:

```ts
await Haptics.cancel();
```

## Complete usage example

```ts
import { Haptics } from "@wefterjs/haptics";

export async function onButtonPressed() {
  await Haptics.impact({ style: "light" });
}

export async function onPaymentCompleted(success: boolean) {
  if (success) {
    await Haptics.notification({ type: "success" });
  } else {
    await Haptics.notification({ type: "error" });
  }
}
```

## Platform implementation notes

### Android

- Uses `android.os.Vibrator` or `android.os.VibratorManager` on Android 12 (API 31) and later.
- Impact and notification patterns map to predefined `VibrationEffect` compositions (`EFFECT_CLICK`, `EFFECT_HEAVY_CLICK`, `EFFECT_TICK`).
- Patterns and amplitude use `VibrationEffect.createWaveform`.

### iOS

- Impact uses `UIImpactFeedbackGenerator` with corresponding `UIImpactFeedbackGenerator.FeedbackStyle`.
- Notification uses `UINotificationFeedbackGenerator`.
- Selection uses `UISelectionFeedbackGenerator`.
- Custom timing patterns fallback to `AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)`.

## License

[MIT](LICENSE) © 2026 Sandip Ghimire
