import { act, renderHook } from "@test/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { clearUnknownOutcome, recordUnknownOutcome, useSubmittingShell, useUnknownOutcome } from "./unknownOutcomeStore";

afterEach(() => {
  clearUnknownOutcome();
});

describe("unknownOutcomeStore", () => {
  it("records a 504 for the shell that is submitting and keeps the label a later call adds", () => {
    const { result } = renderHook(() => {
      useSubmittingShell("shell-1", true);
      return useUnknownOutcome("shell-1");
    });

    act(() => {
      expect(recordUnknownOutcome(undefined)).toBe(true);
      expect(recordUnknownOutcome("Speichern")).toBe(true);
      expect(recordUnknownOutcome(undefined)).toBe(true);
    });

    expect(result.current).toEqual({ shellId: "shell-1", action: "Speichern" });
  });

  it("records nothing while no shell is submitting", () => {
    renderHook(() => useSubmittingShell("shell-1", false));

    expect(recordUnknownOutcome("Speichern")).toBe(false);
  });

  it("forgets a shell that stopped submitting", () => {
    const { rerender } = renderHook((props: { isSubmitting: boolean }) => useSubmittingShell("shell-1", props.isSubmitting), {
      initialProps: { isSubmitting: true },
    });

    rerender({ isSubmitting: false });

    expect(recordUnknownOutcome("Speichern")).toBe(false);
  });
});
