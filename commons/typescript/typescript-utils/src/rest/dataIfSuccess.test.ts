import { describe, expect, it } from "vitest";
import { dataIfSuccess } from "./dataIfSuccess";
import type { RestResponse } from "./RestResponse";

describe("dataIfSuccess", () => {
  it("returns data for a success response", () => {
    const response: RestResponse<string> = { type: "good", data: "hello" };
    expect(dataIfSuccess(response)).toBe("hello");
  });

  it("returns complex data for a success response", () => {
    const data = { id: 1, name: "Test" };
    const response: RestResponse<{ id: number; name: string }> = { type: "good", data };
    expect(dataIfSuccess(response)).toEqual(data);
  });

  it("returns null for a failure response", () => {
    const response: RestResponse<string> = { type: "bad/NotFound" };
    expect(dataIfSuccess(response)).toBeNull();
  });

  it("returns null for a different failure type", () => {
    const response: RestResponse<string> = { type: "bad/Unauthorized" };
    expect(dataIfSuccess(response)).toBeNull();
  });

  it("throws an explanatory error for a subtype-less bad discriminator", () => {
    const response = { type: "bad" } as unknown as RestResponse<string>;
    expect(() => dataIfSuccess(response)).toThrow(/@SerialName/);
  });

  it("throws an explanatory error for a discriminator that violates the contract", () => {
    const response = { type: "alreadyExists" } as unknown as RestResponse<string>;
    expect(() => dataIfSuccess(response)).toThrow(/@SerialName/);
  });

  it("returns undefined for undefined", () => {
    expect(dataIfSuccess(undefined)).toBeUndefined();
  });
});
