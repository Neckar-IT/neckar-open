import { Address } from "@neckar-hub/typescript-utils/address";
import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import type { AddressConfirmed, AddressDraft } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { NoValue } from "../../text/no-value";

interface AddressOutputProps {
  address: AddressDraft | AddressConfirmed | null | undefined;
  /**
   * inline: "Street 1, 12345 City" in a span
   * block: street line and city line in a div
   */
  variant?: "inline" | "block";
  /** Adds the country as a third line of the block. */
  showCountry?: boolean;
  className?: string;
}

/**
 * Formats and displays a postal address, block or inline.
 */
export function AddressOutput({ address, variant = "block", showCountry = false, className }: AddressOutputProps): React.ReactNode {
  if (isNotDefined(address)) {
    return <NoValue className={className} />;
  }

  switch (variant) {
    case "inline": {
      return <span className={cn("whitespace-nowrap", className)}>{Address.toString(address) ?? <NoValue />}</span>;
    }

    case "block":
      return (
        <div className={className}>
          {Address.toStreetAndHouseNumberString(address) ?? <NoValue />}
          <br />
          {Address.toZipCodeAndCityString(address) ?? <NoValue />}
          {showCountry && address.country ? (
            <>
              <br />
              {address.country}
            </>
          ) : null}
        </div>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: variant`);
  }
}
