import { describeShapeCorpus, loadPaymentFormatCases, type ShapeCorpus } from "./paymentFormatsTestSupport";
import { validateMandateReferenceShape, MandateReferenceFormatViolation } from "./validateMandateReferenceShape";

/** `Blank` is a violation of the Kotlin value class alone; the form's required-field check answers an empty field. */
describeShapeCorpus(
  "validateMandateReferenceShape",
  loadPaymentFormatCases<ShapeCorpus<MandateReferenceFormatViolation | "Blank">>(
    "commons/payment/src/jvmTest/resources/MandateReference.cases.json",
  ),
  validateMandateReferenceShape,
  Object.values(MandateReferenceFormatViolation),
);
