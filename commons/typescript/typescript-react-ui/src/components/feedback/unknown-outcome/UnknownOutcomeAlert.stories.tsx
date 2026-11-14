import type { Story, StoryDefault } from "@ladle/react";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { useEffect } from "react";
import { UnknownOutcomeAlert } from "./UnknownOutcomeAlert";
import { recordUnknownOutcome, useSubmittingShell } from "./unknownOutcomeStore";

export default {
  title: "Feedback / Unanswered Mutation Alert",
} satisfies StoryDefault;

/**
 * A form shell whose submit the service accepted and did not answer within the gateway timeout.
 */
function SubmittingShell(): React.ReactElement {
  useSubmittingShell("story-shell", true);
  useEffect(() => {
    recordUnknownOutcome("Verlag speichern");
  }, []);

  return (
    <UnknownOutcomeAlert
      shellId="story-shell"
      onSendAgain={() => console.info("send again")}
      onCloseForm={() => console.info("close form")}
    />
  );
}

/**
 * Alert A after a save the service accepted and did not answer within the gateway timeout.
 */
export const UnansweredSave: Story = () => (
  <QueryClientProvider client={createQueryClient()}>
    <SubmittingShell />
  </QueryClientProvider>
);
