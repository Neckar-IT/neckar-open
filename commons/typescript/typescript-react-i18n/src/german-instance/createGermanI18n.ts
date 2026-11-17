import i18next, { type Resource } from "i18next";

/**
 * Own i18next instance with the German bundle of an app, apart from the singleton a test setup initializes empty
 * so component tests render raw keys.
 */
export async function createGermanI18n(resources: Resource): Promise<ReturnType<typeof i18next.createInstance>> {
  const i18n = i18next.createInstance();
  await i18n.init({
    resources,
    lng: "de",
    fallbackLng: "de",
    defaultNS: "translation",
    interpolation: { escapeValue: false },
  });
  return i18n;
}
