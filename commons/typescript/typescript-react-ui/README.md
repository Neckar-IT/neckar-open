# `typescript-react-ui` - React Component Library

This project contains React Components that can be used in any React Project. The primitives build on the shadcn/ui
components of `@neckar-hub/typescript-react-ui-primitives`; every other component is built here.

**Hosted stories:** [https://react-ui.showcase.neckar.it](https://react-ui.showcase.neckar.it) — the current `main` build
of the Storybook stories is deployed continuously. Run `./gradlew :internal:open:commons:typescript:typescript-react-ui:run`
locally to work on stories — it builds the workspace dependencies first and serves on this
worktree's Storybook port — or `./gradlew :internal:open:commons:typescript:typescript-react-ui:storybookBuild` to produce
the static site served at that URL.

## Structure

All components are located in `src/components` and grouped by function. Each category is one export,
`@neckar-hub/typescript-react-ui/<category>`, backed by `src/components/<category>/index.ts`.

| Category       | Content                                                                                                                                                                           |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `inputs`       | Inputs without form context: `TextInput`, `NumberInput`, `DatePicker`, `Select`, `Combobox`, `Checkbox`, `Switch`                                                                 |
| `filters`      | Filter inputs and filter bars: `FilterByEntity`, `FilterByYear`, `FilterTrigger`, `Filters`                                                                                       |
| `forms`        | React Hook Form binding: `Form`, `FormFieldBase`, every `*FormField`, `Label`, `useUpsertAction`                                                                                  |
| `data-display` | Displaying data: `ResponsiveTable`, `DataTable`, `DescriptionList`, `EntityDetailsList`, `KeyValueGrid`, `NumberOutput`, `ChartContainer`                                         |
| `feedback`     | Status, progress, messages: `ToastProvider`, `LoadingIndicator`, `Empty`, `Badge`, `Alert`, `Skeleton`                                                                            |
| `navigation`   | Orientation: `Pagination`, `Breadcrumbs`, `TimelineNav`, `Tabs`, `Sidebar`, `MultiStep`, `createSidebarNavMenuItem`, `createHeaderNavItem`                                        |
| `layout`       | Containers and Semantic Layout Components: `Page`, `ButtonBar`, `BadgeBar`, `DataContainer`, `Card`, `Accordion`                                                                  |
| `overlays`     | Dialogs, popovers, sheets: `FormDialog`, `SaveDialog`, `ConfirmationDialog`, `Dialog`, `Sheet`, `Tooltip`, `createEntityDialogAction`, `createEntityDialogAtom`, `DialogRegistry` |
| `actions`      | Triggers: `Button`, `AddButton`, `ActionButton`, `DropdownMenu`, `Toggle`                                                                                                         |
| `media`        | Images, icons, uploads: `Image`, `ImageUpload`, `IconWithTooltip`, `Avatar`                                                                                                       |
| `text`         | Text rendering: `HighlightText`, `NoValue`, `EMail`, `PhoneNumber`                                                                                                                |
| `quantities`   | Units: `current`, `energy`, `fraction`, `money`, `percentage`, `power`, `time`, `voltage`                                                                                         |

Every component lives in its own directory `src/components/<category>/<component>/`. Inside the package, import the
component directory (`../inputs/text-input`), never a category barrel.

- `primitives`: The wrapper layer over `@neckar-hub/typescript-react-ui-primitives`, the unmodified output of
  the shadcn CLI. `primitives/<name>/<Name>.tsx` re-exports `@neckar-hub/typescript-react-ui-primitives/ui/<name>`
  or wraps it; every own adjustment (variant, class, prop, default) lives in the wrapper, with a comment giving its
  reason. `range-slider`, `unstyled-button` and `visually-hidden` have no shadcn counterpart and are built here on
  `radix-ui`. The category barrels re-export the wrappers; apps never import `primitives/*` and never the primitives package.
  `Calendar`, `Command`, `Input` and the parts of `Select` are used only inside this package and no category exports them.
- Domain directories with their own export: `entities`, `draft-confirmed`, `price-timeline`, `value-timeline`,
  `conditions-timeline`, `optimistic-locking`, `quote`, `pages/*`.

## Usage

### Dependency

To use these components in your project, add the dependency to your package.json file:

```
dependencies: {
  "@neckar-hub/typescript-react-ui": "workspace:*"
}
```

### Styling with Tailwind

Also you need to configure tailwind with the proper CSS Variables. You can use the Neckar IT Tailwind Configuration and
Styles from `internal/open/config/typescript/tailwind-config`.

### Localization

Finally you need to initialize an instance of i18next in your project. This needs to be done even if you only want to
use one language.

```typescript
// src/lib/i18n.ts

import i18n from "i18next";
import { initReactI18next } from "react-i18next";

i18next.use(initReactI18next).init({
  resources: {},
  lng: "en",
  defaultNS: "translations",
  interpolation: {
    escapeValue: false,
  },
});

export default i18next;
```

Then you should import this file in the main entrypoint of your project.

```typescript
// e.g. src/main.tsx (for vite)

import "./lib/i18n";
```

For more information on how to use localization for the components, please refer to the stories for localization.

For general information about i18next, please refer to the official documentation: https://www.i18next.com/ and
https://react.i18next.com/

### Using the Components

Then you can import the components in your project like this:

```
import { Button } from '@neckar-hub/typescript-react-ui/actions'
```

### Datum, Zeit und physikalische Größen

| Modell-Typ  | Komponente im Formular                                         | Wert im Form-State                             |
| ----------- | -------------------------------------------------------------- | ---------------------------------------------- |
| `Instant`   | `DateTimePickerFormField` (`src/components/inputs/date-time/`) | ISO-8601-String in UTC, `2026-05-11T00:00:00Z` |
| `LocalDate` | `DatePickerFormField` (`src/components/inputs/date/`)          | ISO-8601-Datum, `2026-05-11`                   |

`DatePicker` und `DateTimePicker` arbeiten intern mit dem JavaScript-`Date` aus `react-day-picker`; außerhalb der
`*FormField`-Wrapper kommen sie nicht zum Einsatz. Regel: ADL 0150.

Werte physikalischer Größen kommen in der Basiseinheit der REST-API an
(ADL 0136).
`src/components/quantities/conversionFactors.ts` hält die Faktoren von der Basiseinheit zur Anzeigeeinheit; je
Anzeigeeinheit wendet eine Komponente (`WattOutput`, `KiloWattOutput`, `VoltOutput`, …) ihren Faktor an.

### Add new Components

To add new components, create your component files in the `src` directory.

A component is a directory `src/components/<category>/<component>/` with an `index.ts`. The `index.ts` of the category
(`src/components/<category>/index.ts`) re-exports its public symbols, and `package.json` carries the fixed entry
`"./<category>"`. A directory directly under `src/components` that is no category (`entities`, `quote`, …) needs its own
entry in the `exports` of `package.json`. `pnpm run build` creates the dist files.

## Unit Tests

Unit tests run with Vitest (jsdom environment). Test files live next to the components as
`*.test.ts` / `*.test.tsx`.

**Commands:**

- `pnpm run vitest:only` - run all unit tests once
- `pnpm run vitest:watch` - run the tests in watch mode
- `pnpm exec vitest run --coverage` - run the tests and create a coverage report

The coverage report uses the `v8` provider. It prints a summary table to the terminal and writes an
HTML report to `coverage/index.html` (open it in a browser for line-by-line coverage). The
`coverage/` directory is git-ignored.

## Screenshot Tests

This package screenshots all Storybook stories and compares them against the baselines on the branch
`data/storybook-baselines`. Commands, schedule and baseline workflow:
[playwright-config README](../../../config/typescript/playwright-config/README.md#runs-and-baselines).

**Options:**

- The tag `no-screenshot` skips the screenshot tests of a story, or of every story of a file when the default export
  carries it.

  ```typescript
  const meta = {
    title: "Actions / Button",
    tags: ["no-screenshot"],
  } satisfies Meta;
  ```

- The tag `responsive` adds screenshot tests at mobile and tablet width.

  ```typescript
  const meta = {
    title: "Actions / Button",
    tags: ["responsive"],
  } satisfies Meta;
  ```
