import { LinkIcon } from "lucide-react";
import { Link } from "react-router";
import type { FieldValidationError } from "./validation";

interface ValidationErrorsListProps {
  errors: FieldValidationError[];
}

/**
 * Renders a list of validation errors with optional links.
 * Used in confirm dialogs and hover cards to show field-level validation issues.
 */
export function ValidationErrorsList({ errors }: ValidationErrorsListProps): React.ReactElement {
  return (
    <ul className="mt-1 pl-6">
      {errors.map((validationError, index) => (
        <li key={index} className="list-disc text-sm">
          <span>{validationError.message}</span>
          {validationError.linkTo ? (
            <Link to={validationError.linkTo} className="text-primary">
              <LinkIcon className="ml-1.5 inline size-4" />
            </Link>
          ) : null}
        </li>
      ))}
    </ul>
  );
}
