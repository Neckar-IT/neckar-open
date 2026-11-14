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
  /** The Zod method named by each diagnostic the rule must produce — a bare count would not notice the rule blaming the wrong call. */
  readonly reportedMethods: ReadonlyArray<string>;
}

const PLUGIN_ENTRY = resolve(import.meta.dirname, "../index.mjs");

function expectedMessage(method: string): string {
  return `Zod deprecated the 'message' parameter of '${method}'. Use 'error' instead — same value, same position.`;
}

/** Sorted because oxlint does not emit the diagnostics of one file in source order. */
function messagesOf(diagnostics: Map<string, OxlintDiagnostic[]>, name: string): string[] {
  const found = diagnostics.get(name);
  if (found === undefined) throw new Error(`Fixture '${name}' was not linted`);
  return found.map((diagnostic) => diagnostic.message).sort();
}

function expectedMessages({ reportedMethods }: ViolationFixture): string[] {
  return reportedMethods.map(expectedMessage).sort();
}

function slug(name: string): string {
  return name.replace(/[^a-zA-Z0-9]+/g, "-");
}

/**
 * Writes each fixture to its own .ts file under a fresh temp dir, runs oxlint
 * once against all of them, and returns a `Map<fixtureName, diagnostics>`.
 */
function lintAll(fixtures: ReadonlyArray<Fixture>): Map<string, OxlintDiagnostic[]> {
  const workDir = mkdtempSync(join(tmpdir(), "no-deprecated-zod-message-"));
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
      rules: { "neckar/no-deprecated-zod-message": "error" },
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
    if (diag.code !== "neckar(no-deprecated-zod-message)") continue;
    const name = diag.filename ? fileToName.get(diag.filename) : undefined;
    if (name === undefined) continue;
    result.get(name)!.push(diag);
  }
  return result;
}

const VIOLATIONS: ReadonlyArray<ViolationFixture> = [
  { name: "min with a params object", source: `const s = z.string().min(1, { message: "required" });`, reportedMethods: ["min"] },
  { name: "max with a params object", source: `const s = z.number().max(60, { message: "too large" });`, reportedMethods: ["max"] },
  {
    name: "positive taking params as first argument",
    source: `const s = z.number().positive({ message: "must be positive" });`,
    reportedMethods: ["positive"],
  },
  {
    name: "bare type constructor taking params as first argument",
    source: `const s = z.string({ message: "required" });`,
    reportedMethods: ["string"],
  },
  {
    name: "instanceof with a params object",
    source: `const s = z.instanceof(File, { message: "pick a file" });`,
    reportedMethods: ["instanceof"],
  },
  {
    name: "refine with a params object",
    source: `const s = schema.refine((v) => v.a === v.b, { message: "mismatch" });`,
    reportedMethods: ["refine"],
  },
  {
    name: "refine with path and message",
    source: `const s = schema.refine(check, { path: ["a"], message: "mismatch" });`,
    reportedMethods: ["refine"],
  },
  { name: "enum with a params object", source: `const s = z.enum(BuildingType, { message: "pick one" });`, reportedMethods: ["enum"] },
  {
    name: "nonempty taking params as first argument",
    source: `const s = z.string().nonempty({ message: "required" });`,
    reportedMethods: ["nonempty"],
  },
  {
    name: "shorthand message property",
    source: `const message = "required";\nconst s = z.string().min(1, { message });`,
    reportedMethods: ["min"],
  },
  {
    name: "every check of one chain, not just the first",
    source: `const s = z.string().min(1, { message: "too short" }).max(5, { message: "too long" });`,
    reportedMethods: ["min", "max"],
  },
];

const NON_VIOLATIONS: ReadonlyArray<Fixture> = [
  { name: "min with the error parameter", source: `const s = z.string().min(1, { error: "required" });` },
  { name: "positive with the error parameter", source: `const s = z.number().positive({ error: "must be positive" });` },
  { name: "enum with the error parameter", source: `const s = z.enum(BuildingType, { error: "pick one" });` },
  { name: "message as a plain string argument", source: `const s = z.string().min(1, "required");` },
  { name: "min without a params object", source: `const s = z.string().min(1);` },
  { name: "object shape declaring a message field", source: `const s = z.object({ message: z.string() });` },
  { name: "nested object shape declaring a message field", source: `const s = z.object({ nested: z.object({ message: z.string() }) });` },
  { name: "extend adding a message field", source: `const s = base.extend({ message: z.string() });` },
  { name: "ctx.addIssue carrying an issue message", source: `ctx.addIssue({ code: "custom", message: "missing" });` },
  { name: "react-hook-form setError", source: `form.setError("icon", { type: "required", message: "pick a file" });` },
  { name: "an unrelated call taking an options object", source: `logger.warn("boom", { message: "details" });` },
];

/**
 * Linted alongside the non-violations: an oxlint run that never loaded the plugin
 * reports nothing, and every "accepts …" case would pass without the rule existing.
 */
const CANARY: ViolationFixture = {
  name: "canary — proves the rule ran over this batch",
  source: `const canary = z.string().min(1, { message: "required" });`,
  reportedMethods: ["min"],
};

describe("no-deprecated-zod-message", () => {
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

    it.each(NON_VIOLATIONS)("accepts $name", ({ name }) => {
      expect(messagesOf(diagnostics, name)).toEqual([]);
    });
  });
});
