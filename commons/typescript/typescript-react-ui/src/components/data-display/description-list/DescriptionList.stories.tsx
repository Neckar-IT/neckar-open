import type { Meta, StoryObj } from "@storybook/react-vite";
import { MailIcon } from "lucide-react";
import {
  DescriptionList,
  DescriptionListLabel,
  DescriptionListRow,
  DescriptionListValue,
  DescriptionListValueStack,
} from "./DescriptionList";

const meta = {
  title: "Data display / DescriptionList",
  component: DescriptionList,
} satisfies Meta;

export default meta;

const mockItems = [
  { id: "KPneFOLKAA", label: "Name", value: "John Doe" },
  { id: "M6xoo6mnGm", label: <MailIcon className="h-5 w-5" />, value: "john@email.com" },
  {
    id: "YCkkm0tHij",
    label: "A really long label test",
    value:
      "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.",
  },
  { id: "fCWWhRmxjE", label: "Empty String", value: "" },
  { id: "Xh2WVufLOP", label: "Null Value", value: null },
  { id: "KrbBMcYsBO", label: "Undefined Value", value: undefined },
];

const mockStackedValues = ["SMA Solar Technology", "Fronius", "Kostal"];

export const DescriptionListStory: StoryObj = {
  name: "Description List",
  render: () => {
    return (
      <DescriptionList>
        {mockItems.map((item) => (
          <DescriptionListRow key={item.id}>
            <DescriptionListLabel>{item.label}</DescriptionListLabel>
            <DescriptionListValue>{item.value}</DescriptionListValue>
          </DescriptionListRow>
        ))}
        <DescriptionListRow>
          <DescriptionListLabel>Stacked Values</DescriptionListLabel>
          <DescriptionListValue>
            <DescriptionListValueStack>
              {mockStackedValues.map((value) => (
                <span key={value}>{value}</span>
              ))}
            </DescriptionListValueStack>
          </DescriptionListValue>
        </DescriptionListRow>
      </DescriptionList>
    );
  },
};
