import { isDefined } from "../lang";

/**
 * Determines wether the given function returns a Promise
 */
export function isPromiseLike<T>(v: unknown): v is Promise<T> {
  return isDefined(v) && typeof (v as Promise<T>).then === "function";
}
