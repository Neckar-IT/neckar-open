import { ComponentNotInitializedError, ComponentReassignmentError } from "./CustomErrors";
import type { WrappedComponent } from "./WrappedComponent";

/**
 * Stores a component and provides methods to access it.
 */
export interface ComponentStore<Component extends WrappedComponent> {
  dispose: () => void;
  setComponent: (component: Component) => void;
  getComponent: () => Component | null;
}

/**
 * Default implementation of the `ComponentStore` interface.
 * Does not invoke `dispose` on the component if it is itself disposed.
 */
export class DefaultComponentStore<Component extends WrappedComponent> implements ComponentStore<Component> {
  private component: Component | null = null;

  dispose(): void {
    this.component = null;
  }

  setComponent(component: Component): void {
    if (this.component !== null && this.component !== component) {
      throw new ComponentReassignmentError();
    }
    this.component = component;
  }

  getComponent(): Component {
    if (this.component === null) {
      throw new ComponentNotInitializedError();
    }
    return this.component;
  }

  getComponentOrNull(): Component | null {
    return this.component;
  }
}
