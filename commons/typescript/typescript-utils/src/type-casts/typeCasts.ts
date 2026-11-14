/**
 * Helper Function to cast a string value to a specific property type of an object.
 */

export function castToObjectProps<T extends Record<string, string>>(value: string | undefined, object: T): T[keyof T] | undefined {
  if (Object.values(object).includes(value as T[keyof T])) {
    return value as T[keyof T];
  }

  return undefined;
}
