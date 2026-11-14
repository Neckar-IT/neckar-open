/**
 * Imports
 */

import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";

/**
 * The absolute path of `path` below `test-data/` at the root of the Git repository the test runs in; throws for a missing file, so a
 * renamed or deleted file fails every test that names it.
 */
export function testDataFile(path: string): string {
  const file = join(repositoryRoot(), "test-data", path);
  if (!existsSync(file)) {
    throw new Error(`The test data file ${file} does not exist`);
  }

  return file;
}

/** The nearest directory above the working directory that holds `.git`, a directory in the main checkout and a file in a worktree. */
function repositoryRoot(): string {
  let directory = resolve(process.cwd());
  while (!existsSync(join(directory, ".git"))) {
    const parent = dirname(directory);
    if (parent === directory) {
      throw new Error(`No Git repository above ${process.cwd()}`);
    }
    directory = parent;
  }

  return directory;
}
