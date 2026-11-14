/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { ErrorIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

/**
 * Meta Information
 *
 * The one state specific to the TokenAuthProvider. Its pending state looks exactly like
 * "Keycloak Auth / Authenticating" and is not repeated here.
 */

const meta: StoryDefault = {
  title: "Token Auth",
};

export default meta;

/**
 * Stories
 */

export const InvalidLoginToken: Story = () => {
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenCenter>
      <ErrorIndicator textKey={t("authProvider.invalidLoginToken")} />
    </FullScreenCenter>
  );
};
InvalidLoginToken.storyName = "Invalid login token";
