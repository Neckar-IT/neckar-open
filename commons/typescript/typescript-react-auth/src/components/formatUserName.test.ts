import { describe, expect, it } from "vitest";
import { formatUserName } from "./formatUserName";

describe("formatUserName", () => {
  it.each([
    [{ firstName: "Ada", lastName: "Lovelace" }, "Ada Lovelace"],
    [{ firstName: "Ada", lastName: null }, "Ada"],
    [{ firstName: null, lastName: "Lovelace" }, "Lovelace"],
    [{ firstName: "", lastName: "Lovelace" }, "Lovelace"],
    [{ firstName: "", lastName: "" }, "ada@example.com"],
    [{ firstName: null, lastName: null }, "ada@example.com"],
  ])("formats %o as %s", (user, expected) => {
    expect(formatUserName(user, "ada@example.com")).toBe(expected);
  });
});
