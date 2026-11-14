// Re-export from Patchable module
// Note: The type `Patchable<T>` is automatically exported with the const `Patchable`
export { Patchable, getPatchableValue, isPatchableDelete, isPatchableKeep, isPatchableUpdate, patchable } from "./Patchable";

// Re-export from PatchableRequired module
// Note: The type `PatchableRequired<T>` is automatically exported with the const `PatchableRequired`
export {
  PatchableRequired,
  getPatchableRequiredValue,
  isPatchableRequiredKeep,
  isPatchableRequiredUpdate,
  patchableRequired,
} from "./PatchableRequired";
