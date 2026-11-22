import { describeShapeCorpus, loadPaymentFormatCases, type ShapeCorpus } from "./paymentFormatsTestSupport";
import { validateBicShape, BicFormatViolation } from "./validateBicShape";

describeShapeCorpus(
  "validateBicShape",
  loadPaymentFormatCases<ShapeCorpus<BicFormatViolation>>("customer/src/jvmTest/resources/BIC.cases.json"),
  validateBicShape,
  Object.values(BicFormatViolation),
);
