import { definePlugin } from "@wefterjs/core";

export type ImpactStyle = "light" | "soft" | "medium" | "rigid" | "heavy";
export type NotificationType = "success" | "warning" | "error";

export interface HapticsAvailability {
  available: boolean;
  amplitudeControlSupported: boolean;
}

export interface ImpactOptions {
  style?: ImpactStyle;
  intensity?: number;
}

export interface NotificationOptions {
  type: NotificationType;
}

export interface VibrateOptions {
  duration?: number;
  pattern?: number[];
  amplitude?: number;
  repeat?: boolean;
}

export interface HapticsResult {
  played: true;
}

export interface CancelResult {
  cancelled: true;
}

export const Haptics = definePlugin<{
  isAvailable: () => Promise<HapticsAvailability>;
  impact: (options?: ImpactOptions) => Promise<HapticsResult>;
  notification: (options: NotificationOptions) => Promise<HapticsResult>;
  selection: () => Promise<HapticsResult>;
  vibrate: (options?: VibrateOptions) => Promise<HapticsResult>;
  cancel: () => Promise<CancelResult>;
}>("haptics", {
  isAvailable: true,
  impact: true,
  notification: true,
  selection: true,
  vibrate: true,
  cancel: true,
});
