import { describeShapeCorpus, loadPaymentFormatCases, type ShapeCorpus } from "./paymentFormatsTestSupport";
import { validateIbanShape, IbanFormatViolation } from "./validateIbanShape";

describeShapeCorpus(
  "validateIbanShape",
  loadPaymentFormatCases<ShapeCorpus<IbanFormatViolation>>("customer/src/jvmTest/resources/IBAN.cases.json"),
  validateIbanShape,
  Object.values(IbanFormatViolation),
);
