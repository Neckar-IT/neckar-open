/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { makeAxiosError } from "../axiosErrorFixture";
import { unknownErrorHandler } from "./unknownErrorHandler";

interface UnknownErrorProps {
  error: unknown;
  errorCaughtBy: string;
}

/**
 * Meta Information
 */

export default {
  title: "Error Handling / Default Error Handlers",
} satisfies StoryDefault;

/**
 * Stories
 */

export const UnknownErrorHandler: Story<UnknownErrorProps> = (props) => unknownErrorHandler(props.error, props.errorCaughtBy);
UnknownErrorHandler.args = {
  error: new Error("Unknown Error"),
  errorCaughtBy: "window-error-listener",
};

/**
 * A rejected value that is no Error: the page takes the value as its message.
 */
export const UnknownRejectedString: Story<UnknownErrorProps> = (props) => unknownErrorHandler(props.error, props.errorCaughtBy);
UnknownRejectedString.args = {
  error: "plain string rejection",
  errorCaughtBy: "window-error-listener",
};

/**
 * Axios error that fell through to the unknown handler: shows the request
 * details and trace-link blocks.
 */
export const UnknownAxiosError: Story<UnknownErrorProps> = (props) => unknownErrorHandler(props.error, props.errorCaughtBy);
UnknownAxiosError.args = {
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
};
