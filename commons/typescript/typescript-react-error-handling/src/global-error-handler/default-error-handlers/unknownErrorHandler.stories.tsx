/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { makeAxiosError } from "../axiosErrorFixture";
import { unknownErrorHandler } from "./unknownErrorHandler";

interface UnknownErrorProps {
  error: unknown;
  errorCaughtBy: string;
}

const meta = {
  title: "Error Handling / Default Error Handlers",
} satisfies Meta;

export default meta;

export const UnknownErrorHandler: StoryObj<UnknownErrorProps> = {
  args: {
    error: new Error("Unknown Error"),
    errorCaughtBy: "window-error-listener",
  },
  render: (props: UnknownErrorProps) => unknownErrorHandler(props.error, props.errorCaughtBy),
};

/**
 * A rejected value that is no Error: the page takes the value as its message.
 */
export const UnknownRejectedString: StoryObj<UnknownErrorProps> = {
  args: {
    error: "plain string rejection",
    errorCaughtBy: "window-error-listener",
  },
  render: (props: UnknownErrorProps) => unknownErrorHandler(props.error, props.errorCaughtBy),
};

/**
 * Axios error that fell through to the unknown handler: shows the request
 * details and trace-link blocks.
 */
export const UnknownAxiosError: StoryObj<UnknownErrorProps> = {
  args: {
    error: makeAxiosError({
      status: 500,
      statusText: "Internal Server Error",
      method: "post",
      url: "/api/patterns/books",
      requestData: { title: "The Pragmatic Programmer" },
      responseData: { message: "Unhandled server error" },
      traceId: "0af7651916cd43dd8448eb211c80319c",
    }),
    errorCaughtBy: "global-error-boundary",
  },
  render: (props: UnknownErrorProps) => unknownErrorHandler(props.error, props.errorCaughtBy),
};
