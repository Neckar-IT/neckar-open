/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { memo, useCallback, useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { MultiStepControl, MultiStepControlProvider, useMultiStepControl, useOnNext, type Step } from "./MultiStepControl";

/**
 * Tests
 */

describe("MultiStepControlProvider", () => {
  // Module-stable props so the memoized context value's dependencies do not change
  // across re-renders that are not supposed to disturb the consumers.
  const steps: Step[] = [{ title: "step-1", component: null }];
  const noop = () => {};

  it("invokes an onNext callback that was registered before a provider re-render", async () => {
    const onNextSpy = vi.fn(() => false);

    function StepChild() {
      useOnNext(onNextSpy);
      return null;
    }

    function NextButton() {
      const { handleNext } = useMultiStepControl();
      return (
        <button type="button" onClick={() => void handleNext()}>
          next
        </button>
      );
    }

    function Harness() {
      const [isSubmitting, setIsSubmitting] = useState(false);
      return (
        <>
          <button type="button" onClick={() => setIsSubmitting((value) => !value)}>
            rerender
          </button>
          <MultiStepControlProvider steps={steps} currentStepIndex={0} onNext={noop} onPrevious={noop} isSubmitting={isSubmitting}>
            <StepChild />
            <NextButton />
          </MultiStepControlProvider>
        </>
      );
    }

    render(<Harness />);

    // Force a provider re-render by toggling a prop.
    fireEvent.click(screen.getByText("rerender"));

    // The callback registered before the re-render must still fire.
    fireEvent.click(screen.getByText("next"));

    await vi.waitFor(() => expect(onNextSpy).toHaveBeenCalledTimes(1));
  });

  it("does not re-render context consumers when the provider re-renders with unchanged props", () => {
    let stepChildRenderCount = 0;

    // memo so that a parent re-render alone does not re-render the child — it only re-renders
    // when the context value it consumes actually changes identity.
    const StepChild = memo(function StepChild() {
      stepChildRenderCount++;
      useOnNext(useCallback(() => false, []));
      return null;
    });

    function Harness() {
      const [, setTick] = useState(0);
      return (
        <>
          <button type="button" onClick={() => setTick((value) => value + 1)}>
            tick
          </button>
          <MultiStepControlProvider steps={steps} currentStepIndex={0} onNext={noop} onPrevious={noop}>
            <StepChild />
          </MultiStepControlProvider>
        </>
      );
    }

    render(<Harness />);
    const initialRenderCount = stepChildRenderCount;

    // Re-render the harness (and thus the provider) without changing any context-relevant prop.
    fireEvent.click(screen.getByText("tick"));

    expect(stepChildRenderCount).toBe(initialRenderCount);
  });

  it("registers an onNext subscriber once and keeps it across re-renders with new inline callbacks", () => {
    const addSpy = vi.spyOn(MultiStepControl.prototype, "addOnNextCallback");

    function StepChild() {
      // A fresh inline callback identity on every render, mirroring real usage. The hook must not
      // re-subscribe per render, otherwise the subscriber Set is mutated on each render and
      // executeCallbacks can re-enter callbacks indefinitely.
      useOnNext(() => false);
      return null;
    }

    function Harness() {
      const [isSubmitting, setIsSubmitting] = useState(false);
      return (
        <>
          <button type="button" onClick={() => setIsSubmitting((value) => !value)}>
            rerender
          </button>
          <MultiStepControlProvider steps={steps} currentStepIndex={0} onNext={noop} onPrevious={noop} isSubmitting={isSubmitting}>
            <StepChild />
          </MultiStepControlProvider>
        </>
      );
    }

    render(<Harness />);
    const registrationsAfterMount = addSpy.mock.calls.length;

    // Each click changes the context value, re-rendering StepChild with a new inline callback identity.
    fireEvent.click(screen.getByText("rerender"));
    fireEvent.click(screen.getByText("rerender"));

    expect(addSpy.mock.calls.length).toBe(registrationsAfterMount);

    addSpy.mockRestore();
  });
});

describe("MultiStepControl", () => {
  it("does not re-enter a callback that re-registers itself while it is running", async () => {
    // A step component re-renders while its onNext callback is awaited (e.g. handleNext flips the
    // submitting flag), which makes useOnNext delete and re-add its subscriber. If executeCallbacks
    // iterated the live Set, the freshly added entry would be visited again, re-entering the callback
    // forever and freezing the browser. executeCallbacks must iterate a snapshot instead.
    const control = new MultiStepControl();
    let invocations = 0;

    const callback = async () => {
      invocations += 1;
      // Reproduce the mid-iteration mutation a re-render performs.
      control.deleteOnNextCallback(callback);
      control.addOnNextCallback(callback);
      // Safety valve: bail out deterministically if the loop regresses instead of hanging the test.
      return invocations < 25;
    };
    control.addOnNextCallback(callback);

    const result = await control.handleNext();

    expect(invocations).toBe(1);
    expect(result).toBe(true);
  });
});
