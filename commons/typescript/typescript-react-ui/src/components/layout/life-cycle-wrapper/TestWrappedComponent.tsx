import type { WrappedComponent } from "./WrappedComponent";

/**
 * Only used for tests!
 *
 * DO NOT USE IN PRODUCTION!
 */
export class TestWrappedComponent implements WrappedComponent {
  private holder: HTMLDivElement | null = null;
  private isDisposed = false;

  /**
   * @override
   */
  dispose(): void {
    if (this.holder) {
      this.holder.remove();
      this.holder = null;
    }
    this.isDisposed = true;
  }

  /**
   * @override
   */
  getHolder(): HTMLDivElement {
    if (this.isDisposed) {
      throw new Error("Component has been disposed");
    }
    if (this.holder) {
      return this.holder;
    }
    this.holder = window.document.createElement("div");
    this.holder.innerText = "Hello World!";
    window.document.body.appendChild(this.holder);
    this.holder.id = "holder";
    return this.holder;
  }
}
