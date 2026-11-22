import { readFileSync } from "node:fs";
import path from "node:path";
import { describe, expect, it } from "vitest";

/** `internal/open/`, below which the Kotlin owner of every payment format keeps its corpus in `src/jvmTest/resources/`. */
const OPEN_DIRECTORY = path.resolve(import.meta.dirname, "../../../../..");

/** Reads a corpus by its path below `internal/open/`; the Kotlin test of the value class reads the same file from its classpath. */
export function loadPaymentFormatCases<Corpus>(pathBelowOpen: string): Corpus {
  return JSON.parse(readFileSync(path.join(OPEN_DIRECTORY, pathBelowOpen), "utf8")) as Corpus;
}

/** A shape corpus: each case is valid with the `canonical` value Kotlin stores, or names the `violation` Kotlin reports. */
export interface ShapeCorpus<Violation extends string> {
  purpose: string;
  /** The violations the TypeScript check reports; an input with any other violation passes it and the server answers. */
  frontendDetectable: Violation[];
  cases: { input: string; canonical?: string; violation?: Violation }[];
}

type ShapeValidationResult = { valid: true } | { valid: false; violation: string };

/** Holds a shape check to its corpus: the TypeScript check is never stricter than the Kotlin value class. */
export function describeShapeCorpus<Violation extends string>(
  name: string,
  corpus: ShapeCorpus<Violation>,
  validate: (input: string) => ShapeValidationResult,
  checkedViolations: readonly string[],
): void {
  describe(`${name} against its corpus`, () => {
    const valid = corpus.cases.filter((testCase) => testCase.violation == null);
    const detectable = corpus.cases.filter(
      (testCase) => testCase.violation != null && corpus.frontendDetectable.includes(testCase.violation),
    );
    const deferred = corpus.cases.filter(
      (testCase) => testCase.violation != null && !corpus.frontendDetectable.includes(testCase.violation),
    );

    it.each(valid)("accepts $input", (testCase) => {
      expect(validate(testCase.input)).toEqual({ valid: true });
    });

    it.each(detectable)("rejects $input with $violation", (testCase) => {
      expect(validate(testCase.input)).toEqual({ valid: false, violation: testCase.violation });
    });

    it.each(deferred)("defers $input ($violation) to the server", (testCase) => {
      expect(validate(testCase.input)).toEqual({ valid: true });
    });

    it("covers every detectable violation", () => {
      expect(new Set(detectable.map((testCase) => testCase.violation))).toEqual(new Set(corpus.frontendDetectable));
    });

    it("lists every violation the TypeScript check reports as detectable", () => {
      expect(new Set<string>(corpus.frontendDetectable)).toEqual(new Set(checkedViolations));
    });
  });
}
