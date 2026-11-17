/**
 * Imports
 */

import { createQueryClient, QueryClientProvider, useQueryForStaticData } from "@neckar-hub/typescript-react-query";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { QueriesData } from "./QueriesData";

/**
 * Meta Information
 */

const queryClient = createQueryClient();

const meta = {
  title: "Query Handling / QueriesData",
  decorators: [
    (Story) => (
      <QueryClientProvider client={queryClient}>
        <Story />
      </QueryClientProvider>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Type Declarations
 */

type MockResponse = { type: "good"; data: string } | { type: "bad/NotFound" };

/**
 * Stories
 */

export const GoodStory: StoryObj<typeof meta> = {
  name: "good",
  render: () => {
    const queryResult = useQueryForStaticData({
      queryKey: ["good-query"],
      queryFn: (): MockResponse => {
        return {
          type: "good",
          data: "This is the data!",
        };
      },
    });

    return <QueriesData queries={[queryResult]}>{([data]) => <div>{data}</div>}</QueriesData>;
  },
};

export const BadNotFoundStory: StoryObj<typeof meta> = {
  name: "bad/NotFound",
  render: () => {
    const queryResult = useQueryForStaticData({
      queryKey: ["bad-query"],
      queryFn: (): MockResponse => {
        return {
          type: "bad/NotFound",
        };
      },
    });

    return <QueriesData queries={[queryResult]}>{([data]) => <div>{data}</div>}</QueriesData>;
  },
};
