# Internationalization (i18n) Utilities for React Applications

This package provides everything you need to create react applications with internationalization support.

## Features

- Built-in translations for zod error messages
- Saves the selected language per user (identified by login name) in local storage
- Re-exports the `useTranslation` hook from `react-i18next` for easy access
- Detect the current browser language and use it as default language. If there are no translations for the browser
  language, fall back to English.

The finally used language is determined as follows:

- Use the language saved in local storage for this specific user
- Use the language saved in local storage as default
- Use the browser language

## Installation

Add the following line to the `package.template.json` in your project:

```json
"dependencies": {
  "@neckar-hub/typescript-react-i18n": "workspace:*"
}
```

Also this package requires the following peer dependencies:

```json
"peerDependencies": {
  "react": "${version.npm.react}",
  "react-dom": "${version.npm.react-dom}",
  "zod": "${version.npm.zod}"
}
```

## Usage

1. Define the translation resources in your application in json-files like so:

```json
{
  "hello-world": "Hallo Welt!"
}
```

- The json-file holds the keys alone. `src/locales/languageResources.ts` puts it under the default namespace
  "translation", as in `internal/patterns/admin-ui/src/locales/languageResources.ts`:

```ts
import de from "./de.json";

export const languageResources = {
  de: {
    translation: de,
  },
};
```

- Another namespace needs the namespace in every call of the translation function: `t('namespace:key')`.

2. Wrap your application in the `I18nProvider` component and pass the translation resources as prop:

```jsx
<React.StrictMode>
  <I18nProvider languageResources={languageResources} options={options}>
    <App />
  </I18nProvider>
</React.StrictMode>
```

`I18nProvider` adds the namespace `zod` with the zod error messages to `de` and `en` and passes the resources to the
i18next init function, in the form the i18next documentation describes
(https://www.i18next.com/how-to/add-or-load-translations#add-on-init).

- To customize the i18next instance, you can pass in an `options` (for more info see
  https://www.i18next.com/overview/configuration-options) prop:

```jsx
const options = {
  lng: "de", // e.g. set the default language
};
```

3. Use the `useTranslation` hook to access the translation function in your components:

```jsx
import { useTranslation } from "@neckar-hub/typescript-react-i18n";

function Translated() {
  const { t } = useTranslation();

  return <div>{t("hello-world")}</div>;
}
```

### Get or Set the current language

Use the `useLanguage` and `useSetLanguage` hooks to get or set the current language.

### Localize Dates and Numbers

There are utility hooks that return a function to localize dates and numbers. These functions will automatically use the
currently selected language from i18next.

```jsx
import { useI18nDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";

function LocalizedDate() {
  const dateFormatter = useI18nDateTimeFormatter("date-and-time");

  return <div>{dateFormatter(new Date())}</div>;
}
```

The available formatters are:

- `useI18nCurrencyFormatter`
- `useI18nDateTimeFormatter`
- `useI18nNumberFormatter`
- `useI18nPercentageFormatter`

### Embedded translations

A package that exports components brings its own translations and registers them under a namespace:

```ts
import de from "./de.json";
import en from "./en.json";

const languageResources = { de, en };

export function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: "ui-components", languageResources });
}
```

Both language files must define the same keys, and a sentence must name its `{{placeholders}}`
identically in every language. A key that reaches only one language renders as the key string —
`authProvider.loggingOut` stands in the user interface where a sentence belongs — and a placeholder
the component does not fill under that name is printed verbatim, braces and all.

Two things enforce it:

- `languageResources` constrains each language file to mirror the other's key structure, so the
  mismatch is a type error at the call site.
- `./gradlew verifyTranslationKeyParity`, part of `verifyMergeRequest`, compares the key sets and
  the placeholder names of every `de`/`en` pair in the repository, including the frontends'
  `locales/` directories.
