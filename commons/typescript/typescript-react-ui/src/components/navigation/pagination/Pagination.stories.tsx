/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Pagination, type PaginationProps } from "./Pagination";

/**
 * Meta Information
 */

export default {
  title: "Navigation / Pagination",
  args: {
    disabled: false,
  },
} satisfies StoryDefault<PaginationProps>;

/**
 * Stories
 */

export const Default: Story<PaginationProps> = (args) => {
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
};

export const Disabled: Story<PaginationProps> = (args) => {
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
};
Disabled.args = {
  disabled: true,
};
