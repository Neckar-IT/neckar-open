import { execFileSync } from "node:child_process";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { beforeAll, describe, expect, it } from "vitest";

interface OxlintDiagnostic {
  message: string;
  code?: string;
  filename?: string;
}

interface Fixture {
  readonly name: string;
  readonly source: string;
}

interface ViolationFixture extends Fixture {
  /** The property named by each diagnostic the rule must produce — a bare count would not notice the rule blaming the wrong property. */
  readonly reportedProperties: ReadonlyArray<string>;
}

const PLUGIN_ENTRY = resolve(import.meta.dirname, "../index.mjs");

function expectedMessage(property: string): string {
  return `Property '${property}' must be typed as 'Uuid' (the brand from @neckar-hub/typescript-utils), not a raw type. Allowed: Uuid, Uuid[], Array<Uuid>, Uuid | null, Uuid | undefined, Foo["id"], Foo["uuid"].`;
}

/** Sorted because oxlint does not emit the diagnostics of one file in source order. */
function messagesOf(diagnostics: Map<string, OxlintDiagnostic[]>, name: string): string[] {
  const found = diagnostics.get(name);
  if (found === undefined) throw new Error(`Fixture '${name}' was not linted`);
  return found.map((diagnostic) => diagnostic.message).sort();
}

function expectedMessages({ reportedProperties }: ViolationFixture): string[] {
  return reportedProperties.map(expectedMessage).sort();
}

function slug(name: string): string {
  return name.replace(/[^a-zA-Z0-9]+/g, "-");
}

/**
 * Writes each fixture to its own .ts file under a fresh temp dir, runs oxlint
 * once against all of them, and returns a `Map<fixtureName, diagnostics>`.
 *
 * Batching keeps the suite at one oxlint process per describe block instead of
 * one per case — the dominant cost is pnpm + oxlint startup, not the lint pass.
 */
function lintAll(fixtures: ReadonlyArray<Fixture>): Map<string, OxlintDiagnostic[]> {
  const workDir = mkdtempSync(join(tmpdir(), "no-raw-id-types-"));
  const configFile = join(workDir, ".oxlintrc.json");

  const fileToName = new Map<string, string>();
  const sourceFiles: string[] = [];

  for (const { name, source } of fixtures) {
    const filepath = join(workDir, `${slug(name)}.ts`);
    writeFileSync(filepath, source);
    fileToName.set(filepath, name);
    sourceFiles.push(filepath);
  }

  writeFileSync(
    configFile,
    JSON.stringify({
      jsPlugins: [PLUGIN_ENTRY],
      rules: { "neckar/no-raw-id-types": "error" },
    }),
  );

  let stdout = "";
  try {
    stdout = execFileSync("pnpm", ["exec", "oxlint", "--config", configFile, "--format=json", ...sourceFiles], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
  } catch (err) {
    stdout = (err as { stdout?: string }).stdout ?? "";
  }

  rmSync(workDir, { recursive: true, force: true });

  const result = new Map<string, OxlintDiagnostic[]>();
  for (const name of fileToName.values()) result.set(name, []);

  if (!stdout.trim()) return result;
  const parsed = JSON.parse(stdout) as { diagnostics?: OxlintDiagnostic[] };
  for (const diag of parsed.diagnostics ?? []) {
    if (diag.code !== "neckar(no-raw-id-types)") continue;
    const name = diag.filename ? fileToName.get(diag.filename) : undefined;
    if (name === undefined) continue;
    result.get(name)!.push(diag);
  }
  return result;
}

const VIOLATIONS: ReadonlyArray<ViolationFixture> = [
  { name: "interface property typed as string", source: `interface Foo { customerId: string }`, reportedProperties: ["customerId"] },
  { name: "interface property typed as number", source: `interface Foo { customerId: number }`, reportedProperties: ["customerId"] },
  { name: "interface property typed as bigint", source: `interface Foo { customerId: bigint }`, reportedProperties: ["customerId"] },
  { name: "array of strings for *Ids", source: `interface Foo { customerIds: string[] }`, reportedProperties: ["customerIds"] },
  { name: "Array<string> for *Ids", source: `interface Foo { customerIds: Array<string> }`, reportedProperties: ["customerIds"] },
  { name: "nullable string", source: `interface Foo { parentId: string | null }`, reportedProperties: ["parentId"] },
  { name: "optional union with string", source: `interface Foo { parentId: string | undefined }`, reportedProperties: ["parentId"] },
  { name: "union mixing Uuid and string", source: `interface Foo { parentId: Uuid | string }`, reportedProperties: ["parentId"] },
  { name: "foreign brand type (not Uuid)", source: `interface Foo { customerId: CustomerId }`, reportedProperties: ["customerId"] },
  { name: "type literal property", source: `type Foo = { customerId: string }`, reportedProperties: ["customerId"] },
  { name: "class property", source: `class Foo { customerId: string = ""; }`, reportedProperties: ["customerId"] },
  { name: "optional property typed as string", source: `interface Foo { customerId?: string }`, reportedProperties: ["customerId"] },
  { name: "Uuid suffix property typed as string", source: `interface Foo { customerUuid: string }`, reportedProperties: ["customerUuid"] },
  { name: "Ref suffix property typed as string", source: `interface Foo { customerRef: string }`, reportedProperties: ["customerRef"] },
  {
    name: "indexed access on a key that is not id-shaped",
    source: `interface Foo { customerId: Customer["name"] }`,
    reportedProperties: ["customerId"],
  },
  {
    name: "every offending property of one interface, not just the first",
    source: `interface Foo { customerId: string; name: string; parentId: number }`,
    reportedProperties: ["customerId", "parentId"],
  },
];

const NON_VIOLATIONS: ReadonlyArray<Fixture> = [
  { name: "Uuid type", source: `interface Foo { customerId: Uuid }` },
  { name: "Uuid array", source: `interface Foo { customerIds: Uuid[] }` },
  { name: "Array<Uuid>", source: `interface Foo { customerIds: Array<Uuid> }` },
  { name: "ReadonlyArray<Uuid>", source: `interface Foo { customerIds: ReadonlyArray<Uuid> }` },
  { name: "nullable Uuid", source: `interface Foo { parentId: Uuid | null }` },
  { name: "optional Uuid", source: `interface Foo { parentId: Uuid | undefined }` },
  { name: "Uuid that is both nullable and optional", source: `interface Foo { parentId: Uuid | null | undefined }` },
  { name: "non-id property typed as string", source: `interface Foo { customerName: string }` },
  { name: "property without type annotation", source: `class Foo { customerId = "abc"; }` },
  { name: "property whose suffix is not the ID pattern", source: `interface Foo { paid: boolean }` },
  { name: "property whose name is exactly 'id' (no boundary)", source: `interface Foo { id: string }` },
  { name: "URL-pattern string-literal key", source: `type Routes = { "/users/:userId": { userId: Uuid } };` },
  { name: "setter property typed as function", source: `interface Foo { setBookId: (value: string) => void }` },
  { name: "indexed access type Foo['id']", source: `interface Foo { bookId: Book["id"] }` },
  { name: "indexed access type Foo['uuid']", source: `interface Foo { bookId: Book["uuid"] }` },
];

/**
 * Linted alongside the non-violations: an oxlint run that never loaded the plugin
 * reports nothing, and every "allows …" case would pass without the rule existing.
 */
const CANARY: ViolationFixture = {
  name: "canary — proves the rule ran over this batch",
  source: `interface Canary { canaryId: string }`,
  reportedProperties: ["canaryId"],
};

describe("no-raw-id-types", () => {
  describe("violations", () => {
    let diagnostics: Map<string, OxlintDiagnostic[]>;

    beforeAll(() => {
      diagnostics = lintAll(VIOLATIONS);
    });

    it.each(VIOLATIONS)("flags $name", (fixture) => {
      expect(messagesOf(diagnostics, fixture.name)).toEqual(expectedMessages(fixture));
    });
  });

  describe("non-violations", () => {
    let diagnostics: Map<string, OxlintDiagnostic[]>;

    beforeAll(() => {
      diagnostics = lintAll([...NON_VIOLATIONS, CANARY]);
    });

    it(`flags the ${CANARY.name}`, () => {
      expect(messagesOf(diagnostics, CANARY.name)).toEqual(expectedMessages(CANARY));
    });

    it.each(NON_VIOLATIONS)("allows $name", ({ name }) => {
      expect(messagesOf(diagnostics, name)).toEqual([]);
    });
  });
});
