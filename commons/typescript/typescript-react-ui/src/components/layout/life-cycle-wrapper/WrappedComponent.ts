/**
 * Interface for a wrapped component in the {@link LifeCycleWrapper}
 */
export interface WrappedComponent {
  /**
   * Is called when the component is unmounted.
   */
  dispose: () => void;
  /**
   * Returns the HTML element that will be added to the DOM.
   */
  getHolder: () => HTMLElement;
}
