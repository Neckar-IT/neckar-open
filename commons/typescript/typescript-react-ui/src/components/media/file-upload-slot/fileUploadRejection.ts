import type { TFunction } from "@neckar-hub/typescript-react-i18n";
import axios from "axios";
import { embeddedTranslationNamespace } from "../../../localization/useEmbeddedTranslation";

/** The reason the backend refused a file; accepts the `FileUploadRejection` of every generated client. */
export type FileUploadRejection =
  | { type: "TooLarge"; maximum: number }
  | { type: "WrongContentType"; expected: string; actual: string }
  | { type: "Unreadable" };

/**
 * The message of a refused file. `t` is the caller's; the texts live in the `ui-components` namespace, which
 * `ImageSlot` and `PdfSlot` load while rendering.
 */
export function fileUploadRejectionMessage(rejection: FileUploadRejection, t: TFunction, formatFileSize: (bytes: number) => string): string {
  switch (rejection.type) {
    case "TooLarge":
      return t(`${embeddedTranslationNamespace}:fileUploadRejection.tooLarge`, { maximum: formatFileSize(rejection.maximum) });
    case "WrongContentType":
      return t(`${embeddedTranslationNamespace}:fileUploadRejection.wrongContentType`, { expected: rejection.expected, actual: rejection.actual });
    case "Unreadable":
      return t(`${embeddedTranslationNamespace}:fileUploadRejection.unreadable`);
    default:
      throw new Error(`Unhandled FileUploadRejection: ${String(rejection satisfies never)}`);
  }
}

/**
 * Holds for an upload route alone: its handler answers every refused file as a `FileUploadRejection`, so a 400 there is
 * the answer of `RequestBodyLimit` to a body over the limit, with a body that is not ours.
 */
export function isRequestBodyTooLarge(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 400;
}
