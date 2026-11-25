import type { Address } from "@neckar-hub/typescript-utils/openapi-types";
import type { Action } from "../actions/Action";
import { ActionButton } from "../actions/ActionButton";
import { AddressOutput } from "../data-display/address-output";
import { ButtonBar } from "../layout/button-bar";
import { PageSection, PageSectionHeader, PageSectionHeading } from "../layout/page";
import { NoValue } from "../text/no-value";
import { ConfirmationStatusBadge } from "./ConfirmationStatusBadge";
import { HasValidationErrors } from "./HasValidationErrors";
import type { FieldValidationError } from "./validation";

export interface AddressSectionProps {
  title: string;
  address: Address | null;
  missingLabel: string;
  editAction: Action;
  confirmAction: Action;
  /** What the confirmation of the owner still misses at the address, shown at the heading. */
  validationErrors?: FieldValidationError[];
}

/**
 * Detail-page section for a 0..1 embedded address with its own Draft/Confirmed lifecycle: the address with its
 * confirmation status (or a `NoValue` placeholder when absent), the edit action and the confirm action.
 */
export function AddressSection({
  title,
  address,
  missingLabel,
  editAction,
  confirmAction,
  validationErrors,
}: AddressSectionProps): React.ReactElement {
  return (
    <PageSection>
      <PageSectionHeader>
        <PageSectionHeading level="secondary">
          <HasValidationErrors validationErrors={validationErrors}>{title}</HasValidationErrors>
        </PageSectionHeading>
      </PageSectionHeader>

      <div className="flex flex-col gap-3">
        {address == null ? (
          <NoValue className="text-sm">{missingLabel}</NoValue>
        ) : (
          <div className="flex items-start gap-2">
            <AddressOutput address={address} showCountry className="text-sm" />
            <ConfirmationStatusBadge status={address.type} variant="with-text" />
          </div>
        )}

        <ButtonBar alignment="left">
          <ActionButton action={editAction} variant="outline" />
          <ActionButton action={confirmAction} />
        </ButtonBar>
      </div>
    </PageSection>
  );
}
