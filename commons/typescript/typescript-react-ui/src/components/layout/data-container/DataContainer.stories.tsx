/**
 * Imports
 */
import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../../primitives/button";
import { Input } from "../../primitives/input";
import { DataContainer, type DataContainerProps } from "./DataContainer";

const meta = {
  title: "Layout / Data Container",
  component: DataContainer,
  args: {
    isFetching: false,
    topLeftComponents: <Input placeholder="Search" />,
    topRightComponents: <Button>Create New</Button>,
    children: (
      <div className="flex h-80 items-center justify-center">
        <p>Data</p>
      </div>
    ),
    paginationInfo: {
      pageIndex: 2,
      pageSize: 10,
      pageCount: 5,
      filteredElementsCount: 47,
      totalElementsCount: 123,
    },
    onSetPagination: () => {
      window.alert("Pagination changed");
    },
  },
} satisfies Meta<DataContainerProps>;

export default meta;

export const Default: StoryObj<DataContainerProps> = {
  render: (args: DataContainerProps) => {
    // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
    return <DataContainer {...args} />;
  },
};

export const NoItems: StoryObj<DataContainerProps> = {
  args: {
    paginationInfo: {
      pageIndex: 0,
      pageSize: 10,
      pageCount: 0,
      filteredElementsCount: 0,
      totalElementsCount: 123,
    },
  },
  render: (args: DataContainerProps) => {
    // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
    return <DataContainer {...args} />;
  },
};

export const InvalidPagination: StoryObj<DataContainerProps> = {
  args: {
    paginationInfo: {
      pageIndex: 5,
      pageSize: 10,
      pageCount: 2,
      filteredElementsCount: 13,
      totalElementsCount: 123,
    },
  },
  render: (args: DataContainerProps) => {
    // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
    return <DataContainer {...args} />;
  },
};

export const NoHeader: StoryObj<DataContainerProps> = {
  args: {
    paginationInfo: {
      pageIndex: 5,
      pageSize: 10,
      pageCount: 2,
      filteredElementsCount: 13,
      totalElementsCount: 123,
    },
    topLeftComponents: undefined,
    topRightComponents: undefined,
  },
  render: (args: DataContainerProps) => {
    // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
    return <DataContainer {...args} />;
  },
};
