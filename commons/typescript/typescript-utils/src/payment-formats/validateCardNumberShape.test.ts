import { describeShapeCorpus, loadPaymentFormatCases, type ShapeCorpus } from "./paymentFormatsTestSupport";
import { validateCardNumberShape, CardNumberFormatViolation } from "./validateCardNumberShape";

describeShapeCorpus(
  "validateCardNumberShape",
  loadPaymentFormatCases<ShapeCorpus<CardNumberFormatViolation>>("commons/payment/src/jvmTest/resources/CardNumber.cases.json"),
  validateCardNumberShape,
  Object.values(CardNumberFormatViolation),
);
