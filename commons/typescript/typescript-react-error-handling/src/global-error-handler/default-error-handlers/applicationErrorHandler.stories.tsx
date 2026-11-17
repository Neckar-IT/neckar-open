/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { makeAxiosError } from "../axiosErrorFixture";
import { UnexpectedErrorPage, type UnexpectedErrorPageProps } from "./components/UnexpectedErrorPage";

/**
 * Meta Information
 */

const meta = {
  title: "Error Handling / Default Error Handlers",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const ExpectedApplicationError: StoryObj<UnexpectedErrorPageProps> = {
  args: {
    error: makeAxiosError({ status: 400, traceId: "0af7651916cd43dd8448eb211c80319c" }),
    errorCaughtBy: "global-error-boundary",
    errorCode: "DB-100",
    errorMessage: "Database connection failed due to unforeseen weather conditions. Even more text ",
  },
  render: (props: UnexpectedErrorPageProps) => <UnexpectedErrorPage {...props} />,
};

export const UnexpectedApplicationError: StoryObj<UnexpectedErrorPageProps> = {
  args: {
    error: makeAxiosError({
      status: 500,
      statusText: "Internal Server Error",
      method: "get",
      url: "/api/patterns/books/42",
      responseData: { type: "UnexpectedError", exceptionType: "java.lang.IllegalStateException" },
      traceId: "0af7651916cd43dd8448eb211c80319c",
    }),
    errorCaughtBy: "global-error-boundary",
    exceptionType: "java.lang.IllegalStateException",
  },
  render: (props: UnexpectedErrorPageProps) => <UnexpectedErrorPage {...props} />,
};

const longFallbackMessage =
  "Invalid value [8b4d3a21-6f2c-4d1e-a34e-641719270e16,21455532-06f5-5c41-a34e-641719270e16,c23e03d9-3712-4365-a34e-641719270e16] for query parameter [manufacturerIds]. Value must hold at most 2 ids.";

/**
 * A response body wide enough to break the page layout: every section has to stay inside the same
 * column instead of stretching it to the window width.
 */
export const UnexpectedApplicationErrorWithLongBody: StoryObj<UnexpectedErrorPageProps> = {
  args: {
    error: makeAxiosError({
      status: 400,
      statusText: "Bad Request",
      method: "get",
      url: "/api/pv/wallbox-types/views/table-entries",
      responseData: {
        type: "UnexpectedError",
        fallbackMessage: longFallbackMessage,
        exceptionType: "it.neckar.rest.InvalidQueryParameterException",
      },
      traceId: "0af7651916cd43dd8448eb211c80319c",
    }),
    errorCaughtBy: "global-error-boundary",
    errorMessage: longFallbackMessage,
    exceptionType: "it.neckar.rest.InvalidQueryParameterException",
  },
  render: (props: UnexpectedErrorPageProps) => <UnexpectedErrorPage {...props} />,
};
