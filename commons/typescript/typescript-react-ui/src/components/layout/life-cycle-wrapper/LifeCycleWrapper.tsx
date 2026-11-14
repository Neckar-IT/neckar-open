import { cn } from "cn";
import type { CSSProperties } from "react";
import { useEffect, useRef } from "react";
import type { ComponentStore } from "./ComponentStore";
import { ComponentNotInitializedError, ComponentStoreHasChangedError, ContainerNotInitializedError } from "./CustomErrors";
import type { WrappedComponent } from "./WrappedComponent";

export interface LifeCycleWrapperProps<Component extends WrappedComponent> {
  /**
   * Creates the wrapped component.
   */
  create: () => Component;
  /**
   * Stores and provides the component previously created by `create`.
   */
  componentStore?: ComponentStore<Component> | undefined;
  /**
   * Optional CSS-styles
   */
  style?: CSSProperties | undefined;
  /**
   * Optional class name
   */
  className?: string | undefined;
}

/**
 * Wraps a component in a DIV-element of a well-defined size.
 *
 * The component is linked to the life-cycle of this wrapper.
 * This wrapper creates the component when it is mounted and disposes it when it is unmounted.
 *
 * Note that this wrapper also disposes the component-store when it is unmounted.
 */
export function LifeCycleWrapper<Component extends WrappedComponent>({
  className,
  create,
  componentStore,
  style,
}: LifeCycleWrapperProps<Component>) {
  const componentRef = useRef<Component | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    componentRef.current = create();
    componentStore?.setComponent(componentRef.current);

    // hook holder-element into the DOM and inject styles
    if (containerRef.current === null) throw new ContainerNotInitializedError();
    const holder = componentRef.current.getHolder();
    containerRef.current.appendChild(holder);
    holder.style.position = "absolute";
    holder.style.inset = "0";

    // dispose the component when the wrapper unmounts
    return () => {
      try {
        componentStore?.dispose();
      } catch (e) {
        console.error("Error while disposing component-store", e);
      }
      try {
        componentRef.current?.dispose();
      } finally {
        componentRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- on-mount hook
  }, []);

  useEffect(() => {
    const component = componentRef.current;
    if (component === null) {
      // This effect should run after the component was created.
      // Hence the component must not be null.
      throw new ComponentNotInitializedError();
    }
    if (componentStore === undefined) {
      return;
    }
    const storedComponent = componentStore.getComponent();
    if (storedComponent !== component) {
      // If the store had not changed, its component should be the same as the one we created.
      throw new ComponentStoreHasChangedError();
    }
  }, [componentStore]);

  return (
    <div
      ref={containerRef}
      className={cn("relative h-full min-h-[200px] w-full", className)}
      data-testid="life-cycle-wrapper"
      style={style}
    />
  );
}
