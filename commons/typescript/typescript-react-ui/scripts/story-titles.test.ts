/**
 * Imports
 */

import { readdirSync, readFileSync } from "node:fs";
import { basename, dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";
import ts from "typescript";
import { describe, expect, it } from "vitest";

/**
 * Every story under `src/components/` is titled `<Folder> / <Component>`, optionally followed by deeper segments: the first folder below `src/components/` with a capital first letter and spaces
 * for hyphens (`data-display` → `Data display`), then the component name. The Storybook sidebar then mirrors the folder structure.
 */

const packageRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const componentsDir = join(packageRoot, "src", "components");

describe("story titles", () => {
  const storyFiles = findStoryFiles(componentsDir);

  it("finds the stories", () => {
    expect(storyFiles.length).toBeGreaterThan(100);
  });

  it.each(storyFiles.map((file) => [relative(packageRoot, file), file]))("%s is titled <Folder> / <Component>", (_relativePath, file) => {
    const folder = relative(componentsDir, file).split("/")[0]!;
    const segments = readTitle(file).split(" / ");

    expect(segments[0]).toBe(folderTitle(folder));
    expect(segments.length).toBeGreaterThanOrEqual(2);
    expect(segments.every((segment) => segment.trim() !== "")).toBe(true);
  });
});

function folderTitle(folder: string): string {
  const spaced = folder.replaceAll("-", " ");
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

function findStoryFiles(directory: string): Array<string> {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      return findStoryFiles(path);
    }
    return entry.name.endsWith(".stories.tsx") ? [path] : [];
  });
}

/**
 * Returns the `title` of the object literal that a story file exports as default, directly or through a `const`, or throws if there is none.
 */
function readTitle(file: string): string {
  const source = ts.createSourceFile(file, readFileSync(file, "utf8"), ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);

  for (const statement of source.statements) {
    if (ts.isExportAssignment(statement)) {
      const meta = resolveObjectLiteral(source, statement.expression);
      const title = meta?.properties.find(
        (property): property is ts.PropertyAssignment => ts.isPropertyAssignment(property) && property.name.getText() === "title",
      );
      if (title !== undefined && ts.isStringLiteralLike(title.initializer)) {
        return title.initializer.text;
      }
    }
  }

  throw new Error(`${basename(file)} has no literal title in its default export`);
}

function resolveObjectLiteral(source: ts.SourceFile, expression: ts.Expression): ts.ObjectLiteralExpression | undefined {
  if (ts.isSatisfiesExpression(expression) || ts.isAsExpression(expression)) {
    return resolveObjectLiteral(source, expression.expression);
  }
  if (ts.isObjectLiteralExpression(expression)) {
    return expression;
  }
  if (ts.isIdentifier(expression)) {
    for (const statement of source.statements) {
      if (ts.isVariableStatement(statement)) {
        for (const declaration of statement.declarationList.declarations) {
          if (declaration.name.getText() === expression.text && declaration.initializer !== undefined) {
            return resolveObjectLiteral(source, declaration.initializer);
          }
        }
      }
    }
  }
  return undefined;
}
