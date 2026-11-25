import type { TFunction } from "@neckar-hub/typescript-react-i18n";
import { AxiosError, type AxiosResponse, CanceledError } from "axios";
import { describe, expect, it } from "vitest";
import de from "../../../localization/de.json";
import { fileUploadRejectionMessage, isRequestBodyTooLarge } from "./fileUploadRejection";

/** Resolves a key against the German commons texts; a missing key fails the test, a missing parameter changes the text. */
const t = ((key: string, options: Record<string, string> = {}) => {
  const [namespace, path = ""] = key.split(":");
  expect(namespace).toBe("ui-components");
  const text = path.split(".").reduce<unknown>((node, part) => (node as Record<string, unknown>)[part], de);
  expect(typeof text, key).toBe("string");
  return (text as string).replace(/\{\{(\w+)\}\}/g, (_, name: string) => options[name] ?? `<missing ${name}>`);
}) as unknown as TFunction;
const formatFileSize = (bytes: number) => `${bytes} B`;

function axiosErrorWithStatus(status: number): AxiosError {
  return new AxiosError("Request failed", "ERR_BAD_REQUEST", undefined, undefined, { status } as AxiosResponse);
}

describe("fileUploadRejectionMessage", () => {
  it("names the formatted maximum of a file over the limit", () => {
    expect(fileUploadRejectionMessage({ type: "TooLarge", maximum: 1024 }, t, formatFileSize)).toBe("Die Datei ist größer als 1024 B.");
  });

  it("names the expected and the actual content type", () => {
    expect(fileUploadRejectionMessage({ type: "WrongContentType", expected: "application/pdf", actual: "text/plain" }, t, formatFileSize)).toBe(
      "Die Datei hat den Typ text/plain. Erlaubt sind application/pdf.",
    );
  });

  it("reports an unreadable file", () => {
    expect(fileUploadRejectionMessage({ type: "Unreadable" }, t, formatFileSize)).toBe("Die Datei lässt sich nicht lesen.");
  });
});

describe("isRequestBodyTooLarge", () => {
  it("recognises the 400 of RequestBodyLimit", () => {
    expect(isRequestBodyTooLarge(axiosErrorWithStatus(400))).toBe(true);
  });

  it.each([401, 413, 422, 500])("ignores status %i", (status) => {
    expect(isRequestBodyTooLarge(axiosErrorWithStatus(status))).toBe(false);
  });

  it("ignores a cancelled upload", () => {
    expect(isRequestBodyTooLarge(new CanceledError())).toBe(false);
  });

  it("ignores an error that is no axios error", () => {
    expect(isRequestBodyTooLarge(Object.assign(new Error("network down"), { response: { status: 400 } }))).toBe(false);
  });
});
