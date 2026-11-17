/**
 * Imports
 */

import type { HookFunction } from "@orval/core";
import * as fs from "fs";

/**
 * Post-processes generated files:
 * 1. Removes the `*.msw.ts` file (not useful in our case)
 * 2. Fixes JSDoc indentation issues from Orval
 *
 * Orval v8 types `HookFunction` with rest parameters `(...args: unknown[])`,
 * while the runtime actually invokes the hook with a single argument: the
 * array of generated file paths. We accept the unknown rest args to satisfy
 * the type and narrow to `string[]` internally.
 */
export const afterAllFilesWrite: HookFunction = (...args) => {
  const files = args[0];

  if (!Array.isArray(files) || !files.every((f): f is string => typeof f === "string")) {
    throw new Error("Expected first argument to be an array of file paths");
  }

  for (const file of files) {
    if (file.endsWith(".msw.ts")) {
      // Remove the msw file - it doesn't contain useful information
      fs.unlinkSync(file);
    } else if (file.endsWith(".ts")) {
      // Fix JSDoc indentation in TypeScript files
      fixJsDocIndentation(file);
    }
  }
};

/**
 * Fixes JSDoc issues in generated files:
 * - Incorrect indentation where lines after the opening have fewer spaces
 * - Removes all empty lines from JSDoc blocks
 */
function fixJsDocIndentation(filePath: string) {
  const content = fs.readFileSync(filePath, "utf-8");

  // Match JSDoc blocks and fix their indentation
  // Pattern: capture only horizontal whitespace (spaces/tabs) before /**, not newlines
  const fixed = content.replace(
    /^([ \t]*)\/\*\*\n([\s\S]*?)\*\//gm,
    (match, leadingIndent: string) => {
      // Split the JSDoc into lines
      const lines = match.split("\n");
      const baseIndent = leadingIndent;

      // Process lines: fix indentation and remove empty lines
      const resultLines: string[] = [];

      for (const [index, line] of lines.entries()) {
        if (index === 0) {
          // First line (/**) - keep as is
          resultLines.push(line);
          continue;
        }

        const trimmed = line.trimStart();

        // Skip empty JSDoc lines (just * or whitespace)
        if (trimmed === "" || trimmed === "*") {
          continue;
        }

        // Add base indentation + 1 space for alignment with first *
        resultLines.push(`${baseIndent} ${trimmed}`);
      }

      return resultLines.join("\n");
    },
  );

  if (fixed !== content) {
    fs.writeFileSync(filePath, fixed);
  }
}
