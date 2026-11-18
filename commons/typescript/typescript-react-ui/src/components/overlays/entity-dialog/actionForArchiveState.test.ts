import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import type { Action } from "../../actions/Action";
import { actionForArchiveState } from "./actionForArchiveState";

const archive: Action = { icon: null, label: "archive", onTrigger: () => {} };
const unarchive: Action = { icon: null, label: "unarchive", onTrigger: () => {} };

describe("actionForArchiveState", () => {
  it("offers the archive action for an active entity", () => {
    expect(actionForArchiveState(ArchiveState.Active, { archive, unarchive })).toBe(archive);
  });

  it("offers the unarchive action for an archived entity", () => {
    expect(actionForArchiveState(ArchiveState.Archived, { archive, unarchive })).toBe(unarchive);
  });
});
