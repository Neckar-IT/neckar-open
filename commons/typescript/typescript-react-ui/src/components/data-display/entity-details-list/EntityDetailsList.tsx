import type { ReactElement, ReactNode } from "react";
import { Card, CardContent } from "../../primitives/card";
import { DoNotTruncateTextContent } from "../../text/text-overflow";
import { DescriptionList, DescriptionListLabel, DescriptionListRow, DescriptionListValue } from "../description-list";

export interface EntityDetailsRow {
  label: ReactNode;
  value: ReactNode;
}

interface EntityDetailsListProps {
  /** A row that exists only for one variant is left out of the array. */
  rows: readonly EntityDetailsRow[];
  /** `DescriptionListRow`s rendered after `rows`, for rows a component of their own derives, such as `CustomDataDetailsRows`. */
  children?: ReactNode;
}

/**
 * The fields of one entity as label and value, in a card on its details page.
 */
export function EntityDetailsList({ rows, children }: EntityDetailsListProps): ReactElement {
  return (
    <DoNotTruncateTextContent>
      <Card>
        <CardContent>
          <DescriptionList>
            {rows.map((row, index) => (
              // The position is the key, so a row left out for a variant follows every row whose value holds state.
              <DescriptionListRow key={index}>
                <DescriptionListLabel>{row.label}</DescriptionListLabel>
                <DescriptionListValue>{row.value}</DescriptionListValue>
              </DescriptionListRow>
            ))}
            {children}
          </DescriptionList>
        </CardContent>
      </Card>
    </DoNotTruncateTextContent>
  );
}
