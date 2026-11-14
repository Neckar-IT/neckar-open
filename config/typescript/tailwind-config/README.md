# About

This project defines a tailwindcss configuration.

You can either import it yourself or start from the astro template
(the Astro template of this repository), which already has it wired up.

## Installation

> [!NOTE]
> this has already been done for you if you started from the astro template.

### 1. Update the dev-dependencies

You need to add the following dev-dependencies to your project:

```
"devDependencies": {
  "@neckar-hub/tailwind-config": "workspace:*",
  "@tailwindcss/vite": "catalog:",
  ...
}
```

The version belongs in the central `catalog:` of `pnpm-workspace.yaml`, never as a literal version string in
the `package.json` — `pnpm add @tailwindcss/vite` writes both entries. See
[pnpm-dependencies](../../../../../ai/guides/pnpm-dependencies.md).

### 2. Add tailwindcss vite plugin

Modify your `vite.config.ts` to include the tailwindcss plugin:

```javascript
import tailwindcss from "@tailwindcss/vite";
import {defineConfig} from "vite";

export default defineConfig({
  plugins: [
    tailwindcss()
  ]
})
```

### 3. Import main.css

Import the tailwindcss styles from a CSS file of your own — `src/styles.css` by convention — not from a
TypeScript entry point. The project-local CSS file is where the `@source` directives that tell Tailwind which
files to scan for class names have to live (see [Scanning class names from workspace
libraries](#scanning-class-names-from-workspace-libraries)); a bare import from `src/main.tsx` gives Tailwind
no place to put them.

```css
/* src/styles.css */
@import "@neckar-hub/tailwind-config/main.css";

@source "../src";
@source "../index.html";
```

Then pull that file into your entry point (`src/index.tsx` or `src/main.tsx`):

```javascript
import "./styles.css";
```

Only this way you may use the colors, fonts etc. defined in the tailwind-config.

### 4. Optional: import nit.css instead of main.css

For a "normal/static" web-site (marketing pages, Astro), import `nit.css` instead of `main.css`. It imports
`main.css` and adds opinionated base styles for HTML elements (headings, paragraphs). App frontends that
render their own component markup import `main.css` directly.

```css
/* src/styles.css */
@import "@neckar-hub/tailwind-config/nit.css";
```

| Entry point | Use for | Adds on top of `main.css` |
|-------------|---------|---------------------------|
| `main.css` | App frontends (React) with own component markup | — |
| `nit.css` | Static / marketing sites (Astro) | Styled `h1`–`h6`, `p`, `article` spacing |

### 5. Optional: Use tailwind-classes in your code

Now you are ready to use the tailwind-classes in your code, e.g.:

```javascript
export const MyComponent = () => (
  <div className="bg-primary p-4">
    Hello World
  </div>
);
```

## Color Modes (light / dark)

The default Neckar IT theme provides a light and a dark color mode.

The light mode is enabled by default.

If you want to use the dark mode, you need to add the CSS-class "dark" to the corresponding element.
That element and all its children will then use the dark mode.
Consequently, to enable the dark mode for the whole page, you would add the class to the html-element:

```html
<html class="dark">
  ...
</html>
```

You can explicitly set the light mode by using the class "light" instead.
This way you may nest elements with dark and light modes.

## Extending the theme with project-specific colors

A project keeps the shared theme and adds its own semantic colors on top. There are three levels; pick the
lowest one that fits:

| Level | When | How | Example |
|-------|------|-----|---------|
| No extension | Standard corporate look | `@import ".../main.css"` only | `src/styles.css` of the app |
| Semantic colors | A few domain/status colors (e.g. publication states) | Two-level token pattern in the app's `styles.css` | `patterns/react-frontend/src/styles.css` |
| Full theme | A distinct visual identity | `data-theme` theme file — see "Pluggable Themes" below | `themes/terminal.css` |

The reference for the middle level is `internal/patterns/react-frontend/src/styles.css`. Add project colors with
the same two-level pattern `main.css` uses — raw HSL values in `:root`, Tailwind mapping in `@theme inline`:

```css
@import "@neckar-hub/tailwind-config/main.css";

@source "../src";

:root {
  --publication-draft: hsl(218deg 11% 65%);
  --publication-published: hsl(221.2deg 83.2% 53.3%);
}

@theme inline {
  --color-publication-draft: var(--publication-draft);
  --color-publication-published: var(--publication-published);
}
```

This yields `bg-publication-draft`, `text-publication-published`, etc. Use the two-level pattern (raw value +
`@theme inline` mapping) rather than aliasing built-in Tailwind palette colors directly in `@theme inline`, so a
pluggable theme can override the raw value per `data-theme`. Add a `.dark` block for a project color only when its
light value would be illegible on the dark background (same rule the shared `state-*` colors follow — see the
comment in `main.css`).

Full walk-through: pattern doc [theme-extension](../../../../patterns/docs/typescript/theme-extension.md).

## Overriding a size of a shared component

A shared component writes a literal utility (`max-w-48`) as long as every project uses the same value. Once a
project needs another value, `src/main.css` declares a custom property in `:root` outside `@theme`, and the
component references it as `max-w-(--breadcrumb-max-width)`. The project redeclares the property in its own
`styles.css` after the `@import`:

```css
@import "@neckar-hub/tailwind-config/main.css";

:root {
  --breadcrumb-max-width: 240px;
}
```


## Scanning class names from workspace libraries

Tailwind only generates the utilities it finds in scanned files. An app that renders components from a
workspace library (`@neckar-hub/typescript-react-ui`, `.../typescript-react-auth`, …) imports that
library's **source manifest** by package name. The library declares where its own class names live; the app
declares only its own sources and never names a path inside `node_modules`:

```css
@import "@neckar-hub/tailwind-config/main.css";

@import "@neckar-hub/typescript-react-ui/tailwind-sources.css";

@source "../src";
@source "../index.html";
```

Import the manifest of every Tailwind-carrying library the `package.json` declares. Their own dependencies
come along automatically: `typescript-react-ui/tailwind-sources.css` imports the manifest of
`typescript-react-router`, so an app that imports the first scans the second without naming it.

`./gradlew verifyTailwindSources` (part of `verifyMergeRequest`) fails when a declared library's manifest is
not reached, when a library renders classes without shipping a manifest, or when an `@source` points into
`node_modules`. Full rule and the two constraints on writing a manifest: pattern doc
[tailwind-source-manifest](../../../../patterns/docs/typescript/tailwind-source-manifest.md).

**Never point `@source` at a package root.** A library's package root holds `build/` (the Gradle-downloaded
Node distribution) and a `node_modules/` with symlink cycles. Tailwind scans recursively and does not honor
the target's `.gitignore` across a symlink, which blows up to tens of GB of memory and gets the `vite build`
OOM-killed (exit 137). This is why a manifest lives in `src/` and says `@source "./"` — that bounds the scan
to exactly the files that carry class names.

## Pluggable Themes

`main.css` uses a two-level token pattern:

1. **Raw values** in `:root` / `.light` / `.dark` (`--background`, `--primary`, `--radius`, `--theme-font-sans`, …)
2. **Tailwind mapping** in `@theme inline` (`--color-background: var(--background)`, `--font-sans: var(--theme-font-sans)`, …)

A theme only overrides the raw values, scoped to a `data-theme` attribute on `<html>` — components using semantic utilities (`bg-background`, `text-primary`, `font-display`) pick up the new values automatically.

Available themes in `src/themes/`:

| Theme | File | Color scheme | Character |
|-------|------|--------------|-----------|
| Terminal | `themes/terminal.css` | fixed dark | auto-merge.dev style: deep navy, cyan accent, Outfit + JetBrains Mono |
| Aurora | `themes/aurora.css` | fixed dark | night violet, mint (northern lights) accent, Space Grotesk, faint aurora glow |
| Paper | `themes/paper.css` | fixed light | warm off-white, bordeaux accent, Fraunces serif display |
| Fjord | `themes/fjord.css` | fixed light | cool ice-blue off-white, petrol accent, Sora + Manrope |

Opt in per app by importing the theme CSS after `main.css`:

```css
@import "@neckar-hub/tailwind-config/main.css";
@import "@neckar-hub/tailwind-config/themes/terminal.css";
@import "@neckar-hub/tailwind-config/themes/paper.css";
```

Activate a theme by setting `data-theme="terminal"` (plus the matching `dark`/`light` class) on `<html>`. The `ThemeProvider` from `@neckar-hub/typescript-react-ui/providers/theme` handles attribute, class, and persistence — see the pattern doc [pluggable-theming](../../../../patterns/docs/typescript/pluggable-theming.md).

To add a new theme, copy one of the existing files, change the `data-theme` selector and the raw values, and register it in the consuming app's theme list.
