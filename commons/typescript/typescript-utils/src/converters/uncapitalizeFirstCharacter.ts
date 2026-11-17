/**
 * The return type keeps a literal type literal, so a key built from it stays checkable against a key registry.
 */
export function uncapitalizeFirstCharacter<T extends string>(str: T): Uncapitalize<T> {
  return `${str.charAt(0).toLowerCase()}${str.slice(1)}` as Uncapitalize<T>;
}
