/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../../primitives/button";
import { Input } from "../../primitives/input";
import { DataSliceToolbar } from "./DataSliceToolbar";
import { DataSliceView, type DataSliceViewProps } from "./DataSliceView";

/**
 * Meta Information
 */

const meta = {
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
} satisfies Meta<DataSliceViewProps>;

export default meta;

/**
 * Stories
 */

export const Default: StoryObj<DataSliceViewProps> = {
  render: (args: DataSliceViewProps) => {
    return <DataSliceView {...args} />;
  },
};

export const NoItems: StoryObj<DataSliceViewProps> = {
  args: {
    paginationInfo: {
      pageIndex: 0,
      pageSize: 10,
      pageCount: 0,
      filteredElementsCount: 0,
      totalElementsCount: 0,
    },
  },
  render: (args: DataSliceViewProps) => {
    return <DataSliceView {...args} />;
  },
};

export const NothingFound: StoryObj<DataSliceViewProps> = {
  args: {
    paginationInfo: {
      pageIndex: 0,
      pageSize: 10,
      pageCount: 0,
      filteredElementsCount: 0,
      totalElementsCount: 123,
    },
  },
  render: (args: DataSliceViewProps) => {
    return <DataSliceView {...args} />;
  },
};

export const InvalidPagination: StoryObj<DataSliceViewProps> = {
  args: {
    paginationInfo: {
      pageIndex: 5,
      pageSize: 10,
      pageCount: 2,
      filteredElementsCount: 13,
      totalElementsCount: 123,
    },
  },
  render: (args: DataSliceViewProps) => {
    return <DataSliceView {...args} />;
  },
};

export const NoHeader: StoryObj<DataSliceViewProps> = {
  args: {
    toolbar: undefined,
  },
  render: (args: DataSliceViewProps) => {
    return <DataSliceView {...args} />;
  },
};

export const Pending: StoryObj<typeof meta> = {
  render: () => (
    <DataSliceView pending toolbar={<DataSliceToolbar filters={<Input placeholder="Search" />} actions={<Button>Create New</Button>} />}>
      <div className="flex h-80 items-center justify-center">
        <p className="text-muted-foreground">Skeleton content here</p>
      </div>
    </DataSliceView>
  ),
};
