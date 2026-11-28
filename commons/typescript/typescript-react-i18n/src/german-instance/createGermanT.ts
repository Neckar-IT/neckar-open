import type { Resource, TFunction } from "i18next";
import { createGermanI18n } from "./createGermanI18n";

/**
 * The `t` of the German bundle, typed for the default namespace alone; i18next types the `t` of an instance for every namespace.
 */
export async function createGermanT(resources: Resource): Promise<TFunction<"translation">> {
  const i18n = await createGermanI18n(resources);
  return i18n.t as unknown as TFunction<"translation">;
}
