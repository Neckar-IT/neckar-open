/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";
import { ApartmentIcon } from "./ApartmentIcon";
import { BeakerEmptyIcon } from "./BeakerEmptyIcon.js";
import { BeakerFullIcon } from "./BeakerFullIcon";
import { BeakerHalfFullIcon } from "./BeakerHalfFullIcon.js";
import { CircleLockIcon } from "./CircleLockIcon";
import { CirclePencilIcon } from "./CirclePencilIcon";
import { CircleSendIcon } from "./CircleSendIcon";
import { ConnectorIcon } from "./ConnectorIcon";
import { ConstructionIcon } from "./ConstructionIcon";
import { FileEuroIcon } from "./FileEuroIcon";
import { InverterIcon } from "./InverterIcon";
import { RoofTileIcon } from "./RoofTileIcon";
import { RoomIcon } from "./RoomIcon";
import { SemiDetachedHouseIcon } from "./SemiDetachedHouseIcon";
import { SolarPanelIcon } from "./SolarPanelIcon";
import { StarSolidIcon } from "./StarSolidIcon";
import { TerracedHouseIcon } from "./TerracedHouseIcon";

const meta = {
  title: "Media / CustomIcons",
} satisfies Meta;

export default meta;

/**
 * Icons List
 */

const iconsList = [
  ApartmentIcon,
  BeakerEmptyIcon,
  BeakerHalfFullIcon,
  BeakerFullIcon,
  CircleLockIcon,
  CirclePencilIcon,
  CircleSendIcon,
  ConnectorIcon,
  ConstructionIcon,
  FileEuroIcon,
  InverterIcon,
  RoofTileIcon,
  RoomIcon,
  SemiDetachedHouseIcon,
  SolarPanelIcon,
  StarSolidIcon,
  TerracedHouseIcon,
];

export const CustomIconsStory: StoryObj<typeof meta> = {
  name: "Custom Icons",
  render: () => {
    return (
      <>
        <h2 className="text-lg font-semibold">Collection of custom icons</h2>
        <p className="text-muted-foreground text-sm">These icons can be used similar to Lucide Icons (except the strokeWidth).</p>
        <div className="mt-4 flex flex-wrap gap-4">
          {iconsList.map((Icon, index) => (
            <Tooltip key={index} delayDuration={0}>
              <TooltipTrigger>
                <div className="bg-muted rounded-md p-4">
                  <Icon />
                </div>
              </TooltipTrigger>
              <TooltipContent className="font-mono" side="bottom">
                {Icon.name}
              </TooltipContent>
            </Tooltip>
          ))}
        </div>
      </>
    );
  },
};
