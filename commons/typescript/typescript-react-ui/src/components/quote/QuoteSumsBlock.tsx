import { Trans } from "@neckar-hub/typescript-react-i18n";
import type { QuoteSums } from "@neckar-hub/typescript-utils/openapi-types";
import { Fragment } from "react";
import { embeddedTranslationNamespace, useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { EuroOutput, VatRateOutput } from "../quantities";

interface QuoteSumsBlockProps {
  sums: QuoteSums;
}

/**
 * The sum block below a quote: the subtotal and the discount when there is one, then net, the tax per rate
 * with the net amount it applies to, and gross — so a reader can recalculate the gross from the block itself.
 */
export function QuoteSumsBlock({ sums }: QuoteSumsBlockProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <dl className="ml-auto grid w-fit grid-cols-[max-content_8rem] gap-x-4 gap-y-1 text-sm [&>dd]:text-right">
      {sums.discount === 0 ? null : (
        <>
          <dt>{t("quote.sums.preDiscountNet")}</dt>
          <dd>
            <EuroOutput value={sums.preDiscountNet} />
          </dd>
          <dt>{t("quote.sums.discount")}</dt>
          <dd>
            <EuroOutput value={sums.discount} />
          </dd>
        </>
      )}
      <dt>{t("quote.sums.net")}</dt>
      <dd>
        <EuroOutput value={sums.net} />
      </dd>
      {sums.vatBreakdown.map((line) => (
        <Fragment key={line.vatRate}>
          <dt>
            <Trans
              i18nKey="quote.sums.vatOnNet"
              ns={embeddedTranslationNamespace}
              components={{
                rate: <VatRateOutput value={line.vatRate} />,
                net: <EuroOutput value={line.net} />,
              }}
            />
          </dt>
          <dd>
            <EuroOutput value={line.vatAmount} />
          </dd>
        </Fragment>
      ))}
      <dt className="font-bold">{t("quote.sums.gross")}</dt>
      <dd className="font-bold">
        <EuroOutput value={sums.gross} />
      </dd>
    </dl>
  );
}
