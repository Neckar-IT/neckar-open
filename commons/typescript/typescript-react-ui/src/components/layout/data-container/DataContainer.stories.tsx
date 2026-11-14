/**
 * Imports
 */
import { type Story, type StoryDefault } from "@ladle/react";
import { Button } from "../../primitives/button";
import { Input } from "../../primitives/input";
import { DataContainer, type DataContainerProps } from "./DataContainer";

/**
 * Meta Information
 */

export default {
  title: "Layout / Data Container",
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
} satisfies StoryDefault<DataContainerProps>;

/**
 * Stories
 */

export const Default: Story<DataContainerProps> = (args) => {
  // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
  return <DataContainer {...args} />;
};

export const NoItems: Story<DataContainerProps> = (args) => {
  // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
  return <DataContainer {...args} />;
};
NoItems.args = {
  paginationInfo: {
    pageIndex: 0,
    pageSize: 10,
    pageCount: 0,
    filteredElementsCount: 0,
    totalElementsCount: 123,
  },
};

export const InvalidPagination: Story<DataContainerProps> = (args) => {
  // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
  return <DataContainer {...args} />;
};
InvalidPagination.args = {
  paginationInfo: {
    pageIndex: 5,
    pageSize: 10,
    pageCount: 2,
    filteredElementsCount: 13,
    totalElementsCount: 123,
  },
};

export const NoHeader: Story<DataContainerProps> = (args) => {
  // oxlint-disable-next-line typescript/no-deprecated -- this story exists to render the deprecated DataContainer component
  return <DataContainer {...args} />;
};
NoHeader.args = {
  paginationInfo: {
    pageIndex: 5,
    pageSize: 10,
    pageCount: 2,
    filteredElementsCount: 13,
    totalElementsCount: 123,
  },
  topLeftComponents: undefined,
  topRightComponents: undefined,
};
