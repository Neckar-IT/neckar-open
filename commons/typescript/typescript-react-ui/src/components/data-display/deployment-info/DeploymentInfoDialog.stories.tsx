/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Button } from "../../primitives/button";
import type { DeploymentInfo } from "./deploymentInfo";
import { DeploymentInfoDialog } from "./DeploymentInfoDialog";
import { FullyInjectedDeploymentInfo, NothingInjectedDeploymentInfo, PartiallyInjectedDeploymentInfo } from "./deploymentInfoExamples";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Data display / DeploymentInfoDialog",
};

export default meta;

/**
 * Stories
 *
 * Every story passes its values, so the screenshots stay fixed instead of following the commit the
 * story browser was built from.
 */

function OpenableDeploymentInfoDialog({ deploymentInfo }: { deploymentInfo: DeploymentInfo }): React.ReactElement {
  const [open, setOpen] = useState(true);

  return (
    <>
      <Button onClick={() => setOpen(true)}>Open</Button>

      <DeploymentInfoDialog deploymentInfo={deploymentInfo} open={open} onOpenChange={setOpen} />
    </>
  );
}

export const FullyInjectedStory: Story = () => <OpenableDeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} />;
FullyInjectedStory.storyName = "DeploymentInfoDialog (everything injected)";

export const NothingInjectedStory: Story = () => <OpenableDeploymentInfoDialog deploymentInfo={NothingInjectedDeploymentInfo} />;
NothingInjectedStory.storyName = "DeploymentInfoDialog (nothing injected)";

export const PartiallyInjectedStory: Story = () => <OpenableDeploymentInfoDialog deploymentInfo={PartiallyInjectedDeploymentInfo} />;
PartiallyInjectedStory.storyName = "DeploymentInfoDialog (partially injected)";
