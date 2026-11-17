/**
 * Type Alias to signal that a value is a Uuid.
 */

export type Uuid = string;

/**
 * Use this to make certain properties of an object required, that have been defined as optional.
 */

export type MakeRequired<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>;

/**
 * Use this to require at least one property from a set of properties.
 */

export type RequireAtLeastOne<T> = {
  [K in keyof T]: Required<Pick<T, K>> & Partial<Omit<T, K>>;
}[keyof T];

/**
 * Data Mode designates the form mode (create, draft, confirmed). This will determine which if the form produces a
 * draft (`Draft` and `Create´) or a confirmed (`Confirmed`) object.
 */
export type DataState = "Draft" | "Confirmed";
export const DataState = {
  Draft: "Draft" as const,
  Confirmed: "Confirmed" as const,
};

/**
 * Sometimes the same form page is used for creating, editing a draft, or confirming an object.
 */
export type FormMode = DataState | "Create";
export const FormMode = {
  ...DataState,
  Create: "Create" as const,
};

/**
 * Primitive types
 */
export type Primitive = string | number | boolean | null | undefined | symbol | bigint;
