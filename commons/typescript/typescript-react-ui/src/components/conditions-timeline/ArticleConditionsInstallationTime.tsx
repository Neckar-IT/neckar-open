import type { ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import { parseIsoDurationHours } from "@neckar-hub/typescript-utils/duration";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { HoursOutput } from "../quantities";

/**
 * Displays the estimated installation time of an article — e.g. `8,5 h`.
 *
 * The wire value is an ISO-8601 duration string (`"PT8H30M"`); it is parsed to hours and rendered
 * by {@link HoursOutput}, the commons quantity component whose base unit for time is hours.
 * An article of a mounted type that carries no estimate of its own renders as `NoValue`.
 */
export const ArticleConditionsInstallationTime = entityPropertyRenderer("articleConditions")<ArticleConditionsToDisplay>({
  displayName: "ArticleConditionsInstallationTime",
  render: ({ entity }) => {
    const hours = parseIsoDurationHours(entity.installationTime);
    if (hours === null) return null;

    return <HoursOutput value={hours} />;
  },
});
