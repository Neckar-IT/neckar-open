import { loadPaymentFormatCases } from "./paymentFormatsTestSupport";
import type { ExpiryDateFormatViolation } from "./validateExpiryDateShape";

/** `ExpiryDate.cases.json`: a case with `wire` pins the wire form, and every case is judged at `now`. */
export interface ExpiryDateCorpus {
  purpose: string;
  now: string;
  frontendDetectable: ExpiryDateFormatViolation[];
  cases: { wire?: string; year: number; month: number; violation?: ExpiryDateFormatViolation }[];
}

export const expiryDateCorpus = loadPaymentFormatCases<ExpiryDateCorpus>("commons/payment/src/jvmTest/resources/ExpiryDate.cases.json");
