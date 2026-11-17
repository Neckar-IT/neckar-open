/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { Provider } from "jotai";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useStartRecoveryRound } from "../../app-connectivity/appConnectivityState";
import { ServiceUnavailablePage } from "./serviceUnavailableErrorHandler";

/**
 * Tests
 */

describe("ServiceUnavailablePage back button", () => {
  let originalHistory: History;
  let originalLocation: Location;
  const back = vi.fn();
  const reload = vi.fn();

  function replaceWindowProperty(name: "history" | "location", value: unknown): void {
    Object.defineProperty(window, name, { value, writable: true, configurable: true });
  }

  beforeEach(() => {
    back.mockReset();
    reload.mockReset();
    originalHistory = window.history;
    originalLocation = window.location;
    replaceWindowProperty("location", { href: "https://patterns.localhost/books", reload });
  });

  afterEach(() => {
    replaceWindowProperty("history", originalHistory);
    replaceWindowProperty("location", originalLocation);
  });

  it.each<[string, (() => void) | undefined, number]>([
    ["with recovery, without a reload", () => undefined, 0],
    ["without recovery, reloading the previous page", undefined, 1],
  ])("steps back in the history %s", async (_name, onRecover, reloads) => {
    replaceWindowProperty("history", { back, length: 3 });
    render(<ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} onRecover={onRecover} />);

    await userEvent.setup().click(await screen.findByRole("button", { name: "Back" }));
    window.dispatchEvent(new PopStateEvent("popstate"));

    expect(back).toHaveBeenCalledOnce();
    expect(reload).toHaveBeenCalledTimes(reloads);
  });

  it.each([
    ["with recovery", () => undefined],
    ["without recovery", undefined],
  ])("goes to the application's entry point on a deep link %s", async (_name, onRecover) => {
    replaceWindowProperty("history", { back, length: 1 });
    render(<ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} onRecover={onRecover} />);

    await userEvent.setup().click(await screen.findByRole("button", { name: "Back" }));

    expect(back).not.toHaveBeenCalled();
    expect(window.location.href).toBe(import.meta.env.BASE_URL);
  });
});

describe("ServiceUnavailablePage addition", () => {
  it("promises no continuation by itself without a health poll or an interval of the caller", () => {
    render(<ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} onRecover={() => undefined} />);

    expect(screen.getByText("The application cannot be reached right now.")).toBeInTheDocument();
    expect(screen.queryByText("As soon as the application is ready again, this page continues by itself.")).not.toBeInTheDocument();
  });

  it("promises the continuation when the caller fetches on an interval of its own", () => {
    render(<ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} onRecover={() => undefined} fetchesOnItsOwn={true} />);

    expect(screen.getByText("As soon as the application is ready again, this page continues by itself.")).toBeInTheDocument();
  });
});

describe("ServiceUnavailablePage recovery", () => {
  function StartRound() {
    const startRecoveryRound = useStartRecoveryRound();
    return <button onClick={startRecoveryRound}>next round</button>;
  }

  it("fetches again once per recovery round while it stays mounted, also when the caller renders a new callback", async () => {
    const onRecover = vi.fn();
    const page = () => (
      <Provider>
        <ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} onRecover={() => onRecover()} fetchesOnItsOwn={true} />
        <StartRound />
      </Provider>
    );
    const { rerender } = render(page());
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: "next round" }));
    rerender(page());
    await user.click(screen.getByRole("button", { name: "next round" }));
    rerender(page());

    expect(onRecover).toHaveBeenCalledTimes(2);
  });
});
