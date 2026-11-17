/**
 * Generates the committed component catalog (CATALOG.md) for this package.
 *
 * Single source of truth = the code:
 * - The component set is derived from the package `exports`: atomic-design glob levels
 *   (`primitives`, `atoms`, …) and barrel groups (`actions`, `entities`, …), following
 *   `export *` re-exports and direct `export function/const` declarations.
 * - Each symbol's one-line description is read from the JSDoc on its declaration, falling
 *   back to the leading block comment of its source file.
 *
 * The catalog is never hand-edited. Drift (a component added/removed/renamed, or a
 * description changed without regenerating) is picked up by the Housekeeping schedule
 * (`gitlab-ci.d/schedule/housekeeping.yml`), which runs this generator and opens a merge
 * request with the result.
 *
 * Run with `pnpm catalog:generate` (Node strips the TypeScript types natively).
 */

import { existsSync, readdirSync, readFileSync, realpathSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const PACKAGE_NAME = "@neckar-hub/typescript-react-ui";

const here = dirname(fileURLToPath(import.meta.url));
const packageRoot = join(here, "..");
const componentsDir = join(packageRoot, "src", "components");
const outputFile = join(packageRoot, "CATALOG.md");

/**
 * Functional categories, each one package export `./<category>` backed by `src/components/<category>/index.ts`.
 */
const CATEGORIES: ReadonlyArray<{ dir: string; title: string }> = [
  { dir: "inputs", title: "Inputs" },
  { dir: "filters", title: "Filters" },
  { dir: "forms", title: "Forms" },
  { dir: "data-display", title: "Data display" },
  { dir: "feedback", title: "Feedback" },
  { dir: "navigation", title: "Navigation" },
  { dir: "layout", title: "Layout" },
  { dir: "overlays", title: "Overlays" },
  { dir: "actions", title: "Actions" },
  { dir: "media", title: "Media" },
  { dir: "text", title: "Text" },
  { dir: "quantities", title: "Quantities" },
];

/**
 * Directories exported per component directory (`./pages/*`). One component per subdirectory; the import path is `<package>/<level>/<dir>`.
 */
const GLOB_LEVELS: ReadonlyArray<{ dir: string; title: string }> = [{ dir: "pages", title: "Pages" }];

/**
 * Domain barrels (`./<group>`): one import path, many symbols re-exported from `index.ts`.
 */
const BARREL_GROUPS: ReadonlyArray<{ dir: string }> = [
  { dir: "draft-confirmed" },
  { dir: "entities" },
  { dir: "optimistic-locking" },
  { dir: "quote" },
];

/**
 * Block comments whose whole content is one of these are section separators, not descriptions.
 */
const SECTION_HEADERS = new Set<string>([
  "imports",
  "props",
  "prop",
  "types",
  "type",
  "type declarations",
  "type definitions",
  "types and interfaces",
  "interfaces",
  "interface",
  "constants",
  "constant",
  "helpers",
  "helper",
  "component",
  "components",
  "main component",
  "sub components",
  "subcomponents",
  "variants",
  "variant",
  "context",
  "hooks",
  "hook",
  "utils",
  "utilities",
  "exports",
  "export",
  "styles",
  "style",
  "schema",
  "state",
  "internal",
  "setup",
  "config",
  "configuration",
]);

interface ExportSpecifier {
  name: string;
  /** Resolved source file where the symbol is declared (used to read its JSDoc), or null if unresolved. */
  sourceFile: string | null;
  /** True for type-only exports (`export type { … }`, interfaces) — excluded from the catalog. */
  typeOnly: boolean;
  /** Last segment of the module specifier the symbol is re-exported from (`text-input` for `./text-input`), or null for a declaration in the module itself. */
  fromModule: string | null;
}

interface CatalogEntry {
  name: string;
  importPath: string;
  description: string;
}

const missingDescriptions: string[] = [];
const lowValueDescriptions: string[] = [];

function pascalCase(kebab: string): string {
  return kebab
    .split("-")
    .filter((part) => part.length > 0)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join("");
}

function camelCase(kebab: string): string {
  const pascal = pascalCase(kebab);
  return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

/**
 * Column helpers (`dateColumn`, `selectEntityColumn`, …) build a table column definition. They are
 * standalone factories rather than parts of the table component they sit next to, so they are
 * searchable entries of their own.
 */
export function isColumnHelper(name: string): boolean {
  return /^[a-z][A-Za-z]*Column$/.test(name);
}

/**
 * A catalog export belongs in the catalog when it is a component/class (PascalCase), a column
 * helper, or the directory's primary lowercase API (e.g. `toast` in `feedback/toast`). This drops
 * utility constants and helpers (e.g. `buttonVariants`, `matchesDefaultTimeOfDay`).
 */
function isCatalogComponent(name: string, dir: string): boolean {
  return /^[A-Z]/.test(name) || name === camelCase(dir) || isColumnHelper(name);
}

function listSubdirectories(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .toSorted((a, b) => a.localeCompare(b));
}

function readSourceFile(filePath: string): ts.SourceFile {
  const text = readFileSync(filePath, "utf8");
  return ts.createSourceFile(filePath, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
}

/**
 * Resolves a module specifier (e.g. "./Button", "./money/euros") relative to `fromFile`.
 */
function resolveModuleFile(fromFile: string, module: string): string | null {
  const base = join(dirname(fromFile), module);
  const candidates = [`${base}.tsx`, `${base}.ts`, join(base, "index.tsx"), join(base, "index.ts")];
  return candidates.find((candidate) => existsSync(candidate)) ?? null;
}

/**
 * Follows `export { name } from "…"` chains to the file that declares `name`, so the JSDoc of the declaration is found
 * behind the `index.ts` of a component directory.
 */
function resolveDeclarationFile(file: string, name: string, visited: Set<string> = new Set<string>()): string | null {
  const key = `${file}#${name}`;
  if (visited.has(key)) return null;
  visited.add(key);

  for (const statement of readSourceFile(file).statements) {
    if (ts.isExportDeclaration(statement) && statement.moduleSpecifier && ts.isStringLiteral(statement.moduleSpecifier)) {
      const target = resolveModuleFile(file, statement.moduleSpecifier.text);
      if (statement.exportClause === undefined) {
        const found = target === null ? null : resolveDeclarationFile(target, name, visited);
        if (found !== null) return found;
      } else if (ts.isNamedExports(statement.exportClause)) {
        const element = statement.exportClause.elements.find((candidate) => candidate.name.text === name);
        if (element !== undefined) {
          // A re-export of a package module ends the chain at this file, whose leading comment describes the symbol.
          return target === null ? file : resolveDeclarationFile(target, element.propertyName?.text ?? name, visited);
        }
      }
    } else if (declaresName(statement, name)) {
      return file;
    }
  }
  return null;
}

function declaresName(statement: ts.Statement, name: string): boolean {
  if (
    (ts.isFunctionDeclaration(statement) ||
      ts.isClassDeclaration(statement) ||
      ts.isInterfaceDeclaration(statement) ||
      ts.isTypeAliasDeclaration(statement)) &&
    statement.name
  ) {
    return statement.name.text === name;
  }
  if (ts.isVariableStatement(statement)) {
    return statement.declarationList.declarations.some(
      (declaration) => ts.isIdentifier(declaration.name) && declaration.name.text === name,
    );
  }
  return false;
}

function hasExportModifier(node: ts.Statement): boolean {
  return ts.canHaveModifiers(node) && (ts.getModifiers(node) ?? []).some((modifier) => modifier.kind === ts.SyntaxKind.ExportKeyword);
}

/**
 * Collects every exported symbol of a module, following `export * from "…"` re-exports and reading
 * direct `export function/const/class` declarations as well as `export { … } from "…"` barrels.
 * Each symbol carries the source file of its declaration so its JSDoc can be read.
 */
function moduleExports(file: string, visited: Set<string> = new Set<string>()): ExportSpecifier[] {
  if (visited.has(file)) return [];
  visited.add(file);

  const sourceFile = readSourceFile(file);
  const exports: ExportSpecifier[] = [];

  for (const statement of sourceFile.statements) {
    if (ts.isExportDeclaration(statement)) {
      const module = statement.moduleSpecifier && ts.isStringLiteral(statement.moduleSpecifier) ? statement.moduleSpecifier.text : null;
      if (statement.exportClause && ts.isNamedExports(statement.exportClause)) {
        const target = module === null ? file : resolveModuleFile(file, module);
        const fromModule = module === null ? null : (module.split("/").at(-1) ?? null);
        for (const element of statement.exportClause.elements) {
          const declarationFile =
            target === null ? null : (resolveDeclarationFile(target, element.propertyName?.text ?? element.name.text) ?? target);
          exports.push({
            name: element.name.text,
            sourceFile: declarationFile,
            typeOnly: statement.isTypeOnly || element.isTypeOnly,
            fromModule,
          });
        }
      } else if (statement.exportClause === undefined && module !== null) {
        const target = resolveModuleFile(file, module);
        if (target !== null) exports.push(...moduleExports(target, visited));
      }
    } else if (hasExportModifier(statement)) {
      if ((ts.isFunctionDeclaration(statement) || ts.isClassDeclaration(statement)) && statement.name) {
        exports.push({ name: statement.name.text, sourceFile: file, typeOnly: false, fromModule: null });
      } else if (ts.isVariableStatement(statement)) {
        for (const declaration of statement.declarationList.declarations) {
          if (ts.isIdentifier(declaration.name))
            exports.push({ name: declaration.name.text, sourceFile: file, typeOnly: false, fromModule: null });
        }
      } else if (ts.isInterfaceDeclaration(statement) || ts.isTypeAliasDeclaration(statement)) {
        exports.push({ name: statement.name.text, sourceFile: file, typeOnly: true, fromModule: null });
      }
    }
  }

  return exports;
}

/**
 * Collapses a raw block-comment body into its first sentence, or null if it is a section header.
 */
export function commentToDescription(rawComment: string): string | null {
  const body = rawComment
    .replace(/^\/\*\*?/, "")
    .replace(/\*\/$/, "")
    .split("\n")
    .map((line) => line.replace(/^\s*\*?\s?/, "").trimEnd());

  const paragraph: string[] = [];
  for (const line of body) {
    const trimmed = line.trim();
    if (paragraph.length > 0 && trimmed.length === 0) break;
    if (trimmed.startsWith("@")) break;
    if (/^changelog\b/i.test(trimmed)) break;
    if (trimmed.length > 0) paragraph.push(trimmed);
  }

  const text = paragraph
    .join(" ")
    .replace(/\{@link\s+([^\s|}]+)[^}]*\}/g, "`$1`")
    .replace(/\s+/g, " ")
    .trim();
  if (text.length === 0) return null;

  const normalized = text
    .toLowerCase()
    .replace(/[.:]+$/, "")
    .trim();
  if (SECTION_HEADERS.has(normalized)) return null;

  // First sentence: a terminator followed by whitespace + an uppercase letter, or end of text.
  // The uppercase guard keeps abbreviations like "i.e." / "e.g." from cutting the sentence short.
  const sentenceMatch = text.match(/^(.*?[.!?])(\s+[A-Z]|\s*$)/);
  return (sentenceMatch?.[1] ?? text).trim();
}

/**
 * First JSDoc-style block comment (opening with `/**`) in the trivia starting at `pos`, as a description.
 */
function descriptionFromLeadingComment(sourceText: string, pos: number): string | null {
  const ranges = ts.getLeadingCommentRanges(sourceText, pos) ?? [];
  for (const range of ranges) {
    const raw = sourceText.slice(range.pos, range.end);
    if (!raw.startsWith("/**")) continue;
    const description = commentToDescription(raw);
    if (description) return description;
  }
  return null;
}

/**
 * Description from the JSDoc attached to the top-level declaration named `name`.
 */
function descriptionFromDeclaration(filePath: string, name: string): string | null {
  const sourceFile = readSourceFile(filePath);
  const sourceText = sourceFile.getFullText();

  for (const statement of sourceFile.statements) {
    let matches = false;
    if (ts.isFunctionDeclaration(statement) && statement.name?.text === name) matches = true;
    else if (ts.isClassDeclaration(statement) && statement.name?.text === name) matches = true;
    else if (ts.isVariableStatement(statement)) {
      matches = statement.declarationList.declarations.some(
        (declaration) => ts.isIdentifier(declaration.name) && declaration.name.text === name,
      );
    }
    if (matches) return descriptionFromLeadingComment(sourceText, statement.getFullStart());
  }

  return null;
}

/**
 * Normalizes a string for echo comparison: lowercased, every non-alphanumeric character removed.
 * "Data Container" and "DataContainer" both collapse to "datacontainer".
 */
function normalizeForEcho(text: string): string {
  return text.toLowerCase().replace(/[^a-z0-9]/g, "");
}

/**
 * True when a description carries no information beyond the component name: a noun phrase ending in
 * `Component` or `Root` (shadcn/Radix boilerplate, e.g. `Badge Component`, `Tabs Root`), or the bare
 * name itself (`Sidebar`, `Data Container`). Icon components are exempt from the name-echo rule — for
 * an icon the spelled-out name (`Apartment Icon`) is a complete description.
 */
export function isLowValueDescription(description: string, componentName: string): boolean {
  const withoutTrailingPunctuation = description
    .trim()
    .replace(/[.!?]+$/, "")
    .trimEnd();
  if (/(?:^|\s)(?:component|root)$/i.test(withoutTrailingPunctuation)) return true;
  if (componentName.endsWith("Icon")) return false;
  return normalizeForEcho(withoutTrailingPunctuation) === normalizeForEcho(componentName);
}

/**
 * Description for an exported symbol: its own declaration JSDoc first (per-symbol, so each export
 * in a multi-component file gets its own line), the file's leading block comment as fallback.
 * Records a miss when neither yields a usable one-liner, and a low-value hit when the description
 * only echoes the component name.
 */
function describe(spec: ExportSpecifier, label: string): string {
  if (spec.sourceFile === null) {
    missingDescriptions.push(`${label} (unresolved source file)`);
    return "";
  }

  const description =
    descriptionFromDeclaration(spec.sourceFile, spec.name) ?? descriptionFromLeadingComment(readFileSync(spec.sourceFile, "utf8"), 0);

  if (description === null) {
    missingDescriptions.push(label);
    return "";
  }

  if (isLowValueDescription(description, spec.name)) {
    lowValueDescriptions.push(`${label} → "${description}"`);
  }

  return description;
}

function collectGlobLevel(level: { dir: string; title: string }): CatalogEntry[] {
  const levelDir = join(componentsDir, level.dir);
  const entries: CatalogEntry[] = [];

  for (const subdir of listSubdirectories(levelDir)) {
    const indexFile = join(levelDir, subdir, "index.ts");
    if (!existsSync(indexFile)) continue;

    const importPath = `${PACKAGE_NAME}/${level.dir}/${subdir}`;
    const components = moduleExports(indexFile).filter((spec) => !spec.typeOnly && isCatalogComponent(spec.name, subdir));

    // A dir that exports a component named after itself (e.g. `Select`, `Collapsible`, `Page`) is one
    // compound component — emit a single row and let its sub-parts (SelectItem, …) stay implicit.
    // A dir without an eponymous component (custom-icons, date, toast, …) lists its distinct members.
    const eponymous = components.find((spec) => spec.name === pascalCase(subdir));
    const rows = eponymous ? [eponymous, ...components.filter((spec) => isColumnHelper(spec.name))] : components;
    for (const spec of rows) {
      entries.push({ name: spec.name, importPath, description: describe(spec, `${level.dir}/${subdir} (${spec.name})`) });
    }
  }

  return entries.toSorted((a, b) => a.name.localeCompare(b.name));
}

/**
 * Rows of one category barrel. A symbol re-exported from a component directory (`./text-input`) is one compound component
 * when the directory exports a component named after itself: one row, its sub-parts stay implicit. A directory without such
 * a component lists its distinct members.
 */
function collectCategory(category: { dir: string }): CatalogEntry[] {
  const importPath = `${PACKAGE_NAME}/${category.dir}`;
  const exports = moduleExports(join(componentsDir, category.dir, "index.ts")).filter((spec) => !spec.typeOnly);
  const byModule = new Map<string, ExportSpecifier[]>();
  for (const spec of exports) {
    const module = spec.fromModule ?? "";
    byModule.set(module, [...(byModule.get(module) ?? []), spec]);
  }
  const entries: CatalogEntry[] = [];

  for (const [module, group] of byModule) {
    const isComponentDirectory = /^[a-z]/.test(module);
    const components = group.filter((spec) => isCatalogComponent(spec.name, isComponentDirectory ? module : ""));
    const eponymous = isComponentDirectory ? components.find((spec) => spec.name === pascalCase(module)) : undefined;
    const rows = eponymous ? [eponymous, ...components.filter((spec) => isColumnHelper(spec.name))] : components;
    for (const spec of rows) {
      entries.push({ name: spec.name, importPath, description: describe(spec, `${category.dir} (${spec.name})`) });
    }
  }

  return entries.toSorted((a, b) => a.name.localeCompare(b.name));
}

function collectBarrelGroup(group: { dir: string }): { importPath: string; entries: CatalogEntry[] } {
  const indexFile = join(componentsDir, group.dir, "index.ts");
  const importPath = `${PACKAGE_NAME}/${group.dir}`;
  const entries: CatalogEntry[] = [];

  for (const spec of moduleExports(indexFile)) {
    if (spec.typeOnly) continue;
    entries.push({ name: spec.name, importPath, description: describe(spec, `${group.dir} (${spec.name})`) });
  }

  return { importPath, entries: entries.toSorted((a, b) => a.name.localeCompare(b.name)) };
}

function escapeCell(text: string): string {
  return text.replace(/\|/g, "\\|");
}

function buildMarkdown(): string {
  const lines: string[] = [];

  lines.push("<!-- GENERATED by generate-catalog — do not edit by hand. -->");
  lines.push("<!-- Source: internal/open/commons/typescript/typescript-react-ui/src (every exported component) -->");
  lines.push(
    "<!-- Change the component and run `pnpm catalog:generate`; the Housekeeping schedule opens a merge request when this file drifts. -->",
  );
  lines.push("");
  lines.push("# Component Catalog");
  lines.push("");
  lines.push(`Every exported component in \`${PACKAGE_NAME}\`, grouped by functional category.`);
  lines.push("");
  lines.push("**Check this catalog before implementing a new frontend component or hardcoding markup.** Search by keyword");
  lines.push("(`date`, `select`, `table`, `dialog`, `money`, …) to find an existing component, then import it from the listed path.");
  lines.push("");
  lines.push("Descriptions live as JSDoc on the components; this file is generated from the source — do not edit it directly.");
  lines.push("");

  for (const category of CATEGORIES) {
    const entries = collectCategory(category);
    if (entries.length === 0) continue;
    lines.push(`## ${category.title}`);
    lines.push("");
    lines.push(`Import: \`${PACKAGE_NAME}/${category.dir}\``);
    lines.push("");
    lines.push("| Component | Description |");
    lines.push("| --- | --- |");
    for (const entry of entries) {
      lines.push(`| \`${entry.name}\` | ${escapeCell(entry.description)} |`);
    }
    lines.push("");
  }

  for (const level of GLOB_LEVELS) {
    const entries = collectGlobLevel(level);
    if (entries.length === 0) continue;
    lines.push(`## ${level.title}`);
    lines.push("");
    lines.push("| Component | Import | Description |");
    lines.push("| --- | --- | --- |");
    for (const entry of entries) {
      lines.push(`| \`${entry.name}\` | \`${entry.importPath}\` | ${escapeCell(entry.description)} |`);
    }
    lines.push("");
  }

  lines.push("## Domain exports");
  lines.push("");
  lines.push("Barrel modules of one business domain that export several related symbols under a single import path.");
  lines.push("");
  for (const group of BARREL_GROUPS) {
    const { importPath, entries } = collectBarrelGroup(group);
    if (entries.length === 0) continue;
    lines.push(`### \`${importPath}\``);
    lines.push("");
    lines.push("| Export | Description |");
    lines.push("| --- | --- |");
    for (const entry of entries) {
      lines.push(`| \`${entry.name}\` | ${escapeCell(entry.description)} |`);
    }
    lines.push("");
  }

  return lines.join("\n").replace(/\n+$/, "\n");
}

function reportProblems(): void {
  if (missingDescriptions.length > 0) {
    process.stderr.write(
      `Cannot generate ${outputFile}: ${missingDescriptions.length} exported component(s) lack a one-line description.\n`,
    );
    process.stderr.write("Add a leading JSDoc block comment to each component's main .tsx (or its exported declaration):\n");
    for (const entry of missingDescriptions.toSorted((a, b) => a.localeCompare(b))) {
      process.stderr.write(`  - ${entry}\n`);
    }
  }

  if (lowValueDescriptions.length > 0) {
    process.stderr.write(
      `Cannot generate ${outputFile}: ${lowValueDescriptions.length} component(s) have a placeholder description that only echoes the name ("Root", "<Name> Component", or the bare name).\n`,
    );
    process.stderr.write("Replace each JSDoc with a one-liner stating what the component does or when to use it:\n");
    for (const entry of lowValueDescriptions.toSorted((a, b) => a.localeCompare(b))) {
      process.stderr.write(`  - ${entry}\n`);
    }
  }
}

function main(): void {
  const markdown = buildMarkdown();

  if (missingDescriptions.length > 0 || lowValueDescriptions.length > 0) {
    reportProblems();
    process.exit(1);
  }

  writeFileSync(outputFile, markdown, "utf8");
  process.stdout.write(`Wrote ${outputFile}\n`);
}

// Run generation only as the entry script; importing the module (tests) must not regenerate or exit.
const entryScriptPath = process.argv[1];
if (entryScriptPath !== undefined && realpathSync(entryScriptPath) === fileURLToPath(import.meta.url)) {
  main();
}
