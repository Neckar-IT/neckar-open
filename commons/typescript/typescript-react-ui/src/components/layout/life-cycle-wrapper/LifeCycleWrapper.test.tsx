import { render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import type { PropsWithChildren, ReactNode } from "react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { DefaultComponentStore } from "./ComponentStore";
import { LifeCycleWrapper } from "./LifeCycleWrapper";
import { TestWrappedComponent } from "./TestWrappedComponent";

/**
 * Config
 */
const buttonTestId = "toggle-visibility-button";

/**
 * Tests
 */
describe("LifeCycleWrapper", () => {
  it("should assign component to store and append holder to DOM", () => {
    const component = new TestWrappedComponent();
    const componentStore = new DefaultComponentStore();
    render(<LifeCycleWrapper componentStore={componentStore} create={() => component} />);

    expect(componentStore.getComponent()).toEqual(component);
    expect(component.getHolder()).toBeInTheDocument();
  });

  it("should dispose", async () => {
    const user = userEvent.setup();

    const component = new TestWrappedComponent();
    const componentStore = new DefaultComponentStore();
    render(
      <ToggleVisibilityComponent>
        <LifeCycleWrapper componentStore={componentStore} create={() => component} />
      </ToggleVisibilityComponent>,
    );

    expect(component.getHolder()).toBeInTheDocument();
    await user.click(screen.getByTestId(buttonTestId));
    expect(() => component.getHolder()).toThrow();
    expect(componentStore.getComponentOrNull()).toBeNull();
    expect(() => componentStore.getComponent()).toThrow();
  });
});

type ToggleVisibilityComponentProps = PropsWithChildren<{
  visible?: boolean | undefined;
}>;

function ToggleVisibilityComponent({ visible: defaultVisibility = true, children }: ToggleVisibilityComponentProps): ReactNode {
  const [visible, setVisible] = useState(defaultVisibility);
  return (
    <div>
      <button data-testid={buttonTestId} type="button" onClick={() => setVisible((prev) => !prev)} />
      {visible && <>{children}</>}
    </div>
  );
}
