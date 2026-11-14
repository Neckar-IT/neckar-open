/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useCustomEvent } from "./useCustomEvent";

/**
 * Custom Event
 */

const eventKey = "custom-event";

class MyCustomEvent extends CustomEvent<string> {
  constructor(detail: string) {
    super(eventKey, { detail });
  }
}

const dispatchMyCustomEvent = (detail: string) => {
  window.document.dispatchEvent(new MyCustomEvent(detail));
};

/**
 * Tests
 */

describe("useCustomEvent", () => {
  it("should call the handler when the event is triggered", () => {
    let eventDetail: string | undefined;

    const handler = vi.fn((event: CustomEvent<string>) => {
      eventDetail = event.detail;
    });

    renderHook(() => {
      useCustomEvent<string>(eventKey, handler);
    });

    dispatchMyCustomEvent("Hello, World!");

    expect(handler).toHaveBeenCalled();
    expect(eventDetail).toBe("Hello, World!");
  });

  it("should remove the listener when the component is unmounted", () => {
    const handler = vi.fn();

    const { unmount } = renderHook(() => {
      useCustomEvent(eventKey, handler);
    });

    unmount();

    dispatchMyCustomEvent("Hello, World!");

    expect(handler).not.toHaveBeenCalled();
  });
});
