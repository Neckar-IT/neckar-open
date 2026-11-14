/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { Button } from "../../primitives/button";
import { Input } from "../../primitives/input";
import { DataSliceToolbar } from "./DataSliceToolbar";
import { DataSliceView, type DataSliceViewProps } from "./DataSliceView";

/**
 * Meta Information
 */

const meta: StoryDefault<DataSliceViewProps> = {
  title: "Layout / DataSliceView",
  args: {
    toolbar: <DataSliceToolbar filters={<Input placeholder="Search" />} actions={<Button>Create New</Button>} />,
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
    onPaginationParamsChange: () => {
      window.alert("Pagination changed");
    },
  },
};

export default meta;

/**
 * Stories
 */

export const Default: Story<DataSliceViewProps> = (args) => {
  return <DataSliceView {...args} />;
};

export const NoItems: Story<DataSliceViewProps> = (args) => {
  return <DataSliceView {...args} />;
};
NoItems.args = {
  paginationInfo: {
    pageIndex: 0,
    pageSize: 10,
    pageCount: 0,
    filteredElementsCount: 0,
    totalElementsCount: 0,
  },
};

export const NothingFound: Story<DataSliceViewProps> = (args) => {
  return <DataSliceView {...args} />;
};
NothingFound.args = {
  paginationInfo: {
    pageIndex: 0,
    pageSize: 10,
    pageCount: 0,
    filteredElementsCount: 0,
    totalElementsCount: 123,
  },
};

export const InvalidPagination: Story<DataSliceViewProps> = (args) => {
  return <DataSliceView {...args} />;
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

export const NoHeader: Story<DataSliceViewProps> = (args) => {
  return <DataSliceView {...args} />;
};
NoHeader.args = {
  toolbar: undefined,
};

export const Pending: Story = () => (
  <DataSliceView pending toolbar={<DataSliceToolbar filters={<Input placeholder="Search" />} actions={<Button>Create New</Button>} />}>
    <div className="flex h-80 items-center justify-center">
      <p className="text-muted-foreground">Skeleton content here</p>
    </div>
  </DataSliceView>
);
