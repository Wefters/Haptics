// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { installMockBridge, uninstallMockBridge } from "@wefterjs/core/testing";
import { WefterBridgeError } from "@wefterjs/core";
import { Haptics } from "../src/index.js";

afterEach(() => {
  uninstallMockBridge();
});

describe("Haptics.isAvailable", () => {
  it("resolves with what the native side reports", async () => {
    installMockBridge({
      haptics: (method) => {
        if (method === "isAvailable") return { available: true, amplitudeControlSupported: true };
        throw new Error(`unexpected method ${method}`);
      },
    });

    const result = await Haptics.isAvailable();

    expect(result).toEqual({ available: true, amplitudeControlSupported: true });
  });
});

describe("Haptics.impact", () => {
  it("is callable with no arguments", async () => {
    installMockBridge({
      haptics: (method, payload) => {
        expect(method).toBe("impact");
        expect(payload).toEqual({});
        return { played: true };
      },
    });

    expect(await Haptics.impact()).toEqual({ played: true });
  });

  it("forwards style and intensity", async () => {
    installMockBridge({
      haptics: (_method, payload) => {
        expect(payload).toEqual({ style: "heavy", intensity: 0.8 });
        return { played: true };
      },
    });

    await Haptics.impact({ style: "heavy", intensity: 0.8 });
  });
});

describe("Haptics.notification", () => {
  it("forwards the notification type", async () => {
    installMockBridge({
      haptics: (method, payload) => {
        expect(method).toBe("notification");
        expect(payload).toEqual({ type: "success" });
        return { played: true };
      },
    });

    expect(await Haptics.notification({ type: "success" })).toEqual({ played: true });
  });
});

describe("Haptics.selection", () => {
  it("is callable with no arguments", async () => {
    installMockBridge({
      haptics: (method, payload) => {
        expect(method).toBe("selection");
        expect(payload).toEqual({});
        return { played: true };
      },
    });

    expect(await Haptics.selection()).toEqual({ played: true });
  });
});

describe("Haptics.vibrate", () => {
  it("forwards a plain duration", async () => {
    installMockBridge({
      haptics: (method, payload) => {
        expect(method).toBe("vibrate");
        expect(payload).toEqual({ duration: 300 });
        return { played: true };
      },
    });

    await Haptics.vibrate({ duration: 300 });
  });

  it("forwards a pattern with repeat", async () => {
    installMockBridge({
      haptics: (_method, payload) => {
        expect(payload).toEqual({ pattern: [200, 100, 200], repeat: true });
        return { played: true };
      },
    });

    await Haptics.vibrate({ pattern: [200, 100, 200], repeat: true });
  });
});

describe("Haptics.cancel", () => {
  it("resolves with cancelled", async () => {
    installMockBridge({
      haptics: (method) => {
        expect(method).toBe("cancel");
        return { cancelled: true };
      },
    });

    expect(await Haptics.cancel()).toEqual({ cancelled: true });
  });
});

describe("error propagation", () => {
  it("surfaces a native rejection as a WefterBridgeError", async () => {
    installMockBridge({
      haptics: () => {
        throw new Error("This device has no vibrator");
      },
    });

    const call = Haptics.impact();

    await expect(call).rejects.toBeInstanceOf(WefterBridgeError);
    await expect(call).rejects.toMatchObject({
      code: "MOCK_ERROR",
      message: "This device has no vibrator",
    });
  });
});
