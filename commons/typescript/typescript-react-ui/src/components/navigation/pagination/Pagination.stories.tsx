/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Pagination, type PaginationProps } from "./Pagination";

const meta = {
  title: "Navigation / Pagination",
  component: Pagination,
  args: {
    disabled: false,
  },
} satisfies Meta<PaginationProps>;

export default meta;

export const Default: StoryObj<PaginationProps> = {
  render: (args: PaginationProps) => {
    const [paginationParams, setPaginationParams] = useState({ pageIndex: 0, pageSize: 10 });

    const totalItems = 87;
    const pageCount = Math.ceil(totalItems / paginationParams.pageSize);

    return (
      <Pagination
        {...args}
        pageCount={pageCount}
        pageIndex={paginationParams.pageIndex}
        pageSize={paginationParams.pageSize}
        onChange={(pageIndex, pageSize) => {
          setPaginationParams({ pageIndex, pageSize });
        }}
      />
    );
  },
};

export const Disabled: StoryObj<PaginationProps> = {
  args: {
    disabled: true,
  },
  render: (args: PaginationProps) => {
    const [paginationParams, setPaginationParams] = useState({ pageIndex: 0, pageSize: 10 });

    const totalItems = 87;
    const pageCount = Math.ceil(totalItems / paginationParams.pageSize);

    return (
      <Pagination
        {...args}
        pageCount={pageCount}
        pageIndex={paginationParams.pageIndex}
        pageSize={paginationParams.pageSize}
        onChange={(pageIndex, pageSize) => {
          setPaginationParams({ pageIndex, pageSize });
        }}
      />
    );
  },
};
