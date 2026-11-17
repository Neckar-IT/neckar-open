/**
 * Imports
 */

import { ErrorIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

/**
 * Meta Information
 *
 * The one state specific to the TokenAuthProvider. Its pending state looks exactly like
 * "Auth Screens / Authenticating" and is not repeated here.
 */

const meta = {
  title: "Token Auth",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const InvalidLoginToken: StoryObj<typeof meta> = {
  name: "Invalid login token",
  render: () => {
    const { t } = useEmbeddedTranslation();

    return (
      <FullScreenCenter>
        <ErrorIndicator textKey={t("authProvider.invalidLoginToken")} />
      </FullScreenCenter>
    );
  },
};
