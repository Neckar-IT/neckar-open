import { AlertCircleIcon } from "lucide-react";
import { HoverCard, HoverCardContent, HoverCardTrigger } from "../primitives/hover-card";
import type { FieldValidationError } from "./validation";
import { ValidationErrorsHoverContent } from "./ValidationErrorsHoverContent";

interface HasValidationErrorsProps extends React.PropsWithChildren {
  validationErrors: FieldValidationError[] | undefined;
}

/**
 * HasValidationErrors adds an alert icon and a hover card with validation error details to its children when validation errors are present.
 * Used in draft entities details views to indicate fields with validation errors.
 */
export function HasValidationErrors({ children, validationErrors }: HasValidationErrorsProps): React.ReactElement {
  if (validationErrors === undefined || validationErrors.length === 0) {
    return <>{children}</>;
  }

  return (
    <HoverCard openDelay={10} closeDelay={100}>
      <HoverCardTrigger className="flex items-center gap-3">
        <span>{children}</span>
        <AlertCircleIcon className="size-4" />
      </HoverCardTrigger>
      <HoverCardContent side="bottom" align="start" sideOffset={8} className="w-sm font-normal">
        <ValidationErrorsHoverContent errors={validationErrors} />
      </HoverCardContent>
    </HoverCard>
  );
}
