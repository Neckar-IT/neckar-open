export class ContainerNotInitializedError extends Error {
  constructor() {
    super("The container for the component was used before it was initialized.");
    this.name = "ContainerNotInitializedError";
  }
}

export class ComponentNotInitializedError extends Error {
  constructor() {
    super("The component was accessed before it was initialized.");
    this.name = "ComponentNotInitializedError";
  }
}

export class ComponentStoreHasChangedError extends Error {
  constructor() {
    super("The store must not change once the component was initialized.");
    this.name = "ComponentStoreHasChangedError";
  }
}

export class ComponentReassignmentError extends Error {
  constructor() {
    super("The component hold by the store must not change.");
    this.name = "ComponentReassignmentError";
  }
}
