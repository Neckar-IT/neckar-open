/**
 * Imports
 */

import { ArrowLeftIcon } from "lucide-react";
import { NotFoundIndicator } from "../components/feedback/not-found-indicator";
import { Button } from "../components/primitives/button";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { type BadResponseType, BadResponseError } from "./BadResponseError";

/**
 * Fallback component for `BadResponseError`. Use this together with `requireGoodResponse`:
 *
 * ```
 * try {
 *   requireGoodResponse(queryResultSuccess);
 *   return <PureComponent data={queryResultSuccess.response.data} />
 * } catch (error) {
 *   return <BadResponseFallback error={error} />
 * }
 * ```
 */

export function BadResponseFallback({ error }: { error: unknown }): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  // The error is not a BadResponseError => rethrow and let another error boundary handle it
  if (!(error instanceof BadResponseError)) {
    throw error;
  }

  // navigate to the previous page
  const handleNavigate = () => {
    window.history.back();
  };

  const notFoundLabel = error.entityLabel
    ? t({ key: "notFoundIndicator.entityNotFound", options: { entityLabel: t(error.entityLabel) } })
    : (error.message ?? undefined);

  // rethrow error for all bad-response types that do not report a missing entity
  if (!isNotFound(error.badType)) {
    throw error;
  }

  return (
    <NotFoundIndicator
      className="flex-1"
      textKey={notFoundLabel}
      labelComponent={
        <div className="flex flex-col items-center gap-4">
          {notFoundLabel ? <span>{notFoundLabel}</span> : t("notFoundIndicator.label")}
          <Button variant="outline" onClick={handleNavigate}>
            <ArrowLeftIcon />
            <span>{t("notFoundIndicator.goBack")}</span>
          </Button>
        </div>
      }
    />
  );
}

/**
 * Whether a bad-response type reports that an entity does not exist.
 *
 * Covers both the shared `bad/NotFound` of `DeleteResponse` and the entity-specific types every
 * response union of its own declares — `bad/InverterConfigurationNotFound`,
 * `bad/RoofConfigurationNotFound`, … — which name the missing entity but mean the same thing.
 * Anything else is a domain failure the calling UI has to handle itself and is rethrown.
 */
function isNotFound(badType: BadResponseType): boolean {
  return badType.endsWith("NotFound");
}
