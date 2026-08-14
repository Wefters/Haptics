# @wefterjs/haptics

Official Wefter plugin for physical device haptic vibration feedback on Android & iOS.

---

## Features

- 📳 **Impact Feedback**: Triggers light, medium, or heavy physical impact haptics.
- 🔔 **Notification Haptics**: Success, warning, and error haptic feedback sequences.
- 🎯 **Selection Haptics**: Subtle haptic feedback for UI sliders, pickers, and toggles.
- ⏱️ **Custom Vibrations**: Custom duration vibration motor feedback.

---

## Installation & Setup

1. Add the plugin to your Wefter project:

```bash
wefter add @wefterjs/haptics
```

2. Synchronize native projects:

```bash
wefter sync
```

---

## Native Permissions Configuration

- **Android** (`AndroidManifest.xml`): Automatically requests `<uses-permission android:name="android.permission.VIBRATE" />`.
- **iOS**: No permissions required (uses `UIImpactFeedbackGenerator` & `UINotificationFeedbackGenerator`).

---

## JavaScript API Reference

Import `invokeNative` from `@wefterjs/core`:

```ts
import { invokeNative } from "@wefterjs/core";
```

### 1. `impact(options)`

Triggers collision or impact haptic feedback.

```ts
type ImpactStyle = "light" | "medium" | "heavy";

await invokeNative("haptics", "impact", { style: "medium" });
```

### 2. `notification(options)`

Triggers notification feedback haptics.

```ts
type NotificationType = "success" | "warning" | "error";

await invokeNative("haptics", "notification", { type: "success" });
```

### 3. `selection()`

Triggers subtle haptic feedback for UI selection changes (e.g. wheel picker, dropdown).

```ts
await invokeNative("haptics", "selection");
```

### 4. `vibrate(options)`

Triggers a custom duration vibration.

```ts
interface VibrateOptions {
  duration?: number; // Duration in milliseconds (default: 300ms)
}

await invokeNative("haptics", "vibrate", { duration: 500 });
```

### 5. `cancel()`

Immediately stops active ongoing vibrations.

```ts
await invokeNative("haptics", "cancel");
```

---

## Complete Usage Example

```ts
import { invokeNative } from "@wefterjs/core";

// Button click feedback
export function onButtonClick() {
  invokeNative("haptics", "impact", { style: "light" });
}

// Payment success feedback
export function onPaymentSuccess() {
  invokeNative("haptics", "notification", { type: "success" });
}
```
