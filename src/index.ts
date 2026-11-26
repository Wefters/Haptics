/** Haptic feedback and vibration. */

import { definePlugin } from "@wefterjs/core";

/** Strength of an impact, from `light` to `heavy`. */
export type ImpactStyle = "light" | "soft" | "medium" | "rigid" | "heavy";
/** Outcome a notification haptic communicates. */
export type NotificationType = "success" | "warning" | "error";

/** Whether the device can vibrate, and whether vibration strength can be controlled. */
export interface HapticsAvailability {
  available: boolean;
  amplitudeControlSupported: boolean;
}

/** Impact style, or an exact `intensity` from 0 to 1 that overrides it. */
export interface ImpactOptions {
  style?: ImpactStyle;
  intensity?: number;
}

/** The kind of notification feedback. */
export interface NotificationOptions {
  type: NotificationType;
}

/**
 * A single vibration of `duration` milliseconds (default 200), or a `pattern` of alternating wait and
 * vibrate durations in milliseconds, optionally repeated until {@link Haptics.cancel}. `amplitude` is the
 * strength from 0 to 1.
 */
export interface VibrateOptions {
  duration?: number;
  pattern?: number[];
  amplitude?: number;
  repeat?: boolean;
}

/** Returned when the feedback was played. */
export interface HapticsResult {
  played: true;
}

/** Returned when vibration was stopped. */
export interface CancelResult {
  cancelled: true;
}

/** The Haptics plugin. */
export const Haptics = definePlugin<{
  /** Whether haptics are supported. */
  isAvailable: () => Promise<HapticsAvailability>;
  /** A short tap, as when something snaps into place. */
  impact: (options?: ImpactOptions) => Promise<HapticsResult>;
  /** Feedback for a success, warning or error. */
  notification: (options: NotificationOptions) => Promise<HapticsResult>;
  /** A light tick, as when a picker value changes. */
  selection: () => Promise<HapticsResult>;
  /** A vibration or vibration pattern. */
  vibrate: (options?: VibrateOptions) => Promise<HapticsResult>;
  /** Stops any vibration in progress. */
  cancel: () => Promise<CancelResult>;
}>("haptics", {
  isAvailable: true,
  impact: true,
  notification: true,
  selection: true,
  vibrate: true,
  cancel: true,
});
