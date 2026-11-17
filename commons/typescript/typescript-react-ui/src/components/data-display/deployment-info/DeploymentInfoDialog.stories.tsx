/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../../primitives/button";
import type { DeploymentInfo } from "./deploymentInfo";
import { DeploymentInfoDialog } from "./DeploymentInfoDialog";
import { FullyInjectedDeploymentInfo, NothingInjectedDeploymentInfo, PartiallyInjectedDeploymentInfo } from "./deploymentInfoExamples";

/**
 * Meta Information
 */

const meta = {
  title: "Data display / DeploymentInfoDialog",
} satisfies Meta;

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

export const FullyInjectedStory: StoryObj<typeof meta> = {
  name: "DeploymentInfoDialog (everything injected)",
  render: () => <OpenableDeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} />,
};

export const NothingInjectedStory: StoryObj<typeof meta> = {
  name: "DeploymentInfoDialog (nothing injected)",
  render: () => <OpenableDeploymentInfoDialog deploymentInfo={NothingInjectedDeploymentInfo} />,
};

export const PartiallyInjectedStory: StoryObj<typeof meta> = {
  name: "DeploymentInfoDialog (partially injected)",
  render: () => <OpenableDeploymentInfoDialog deploymentInfo={PartiallyInjectedDeploymentInfo} />,
};
