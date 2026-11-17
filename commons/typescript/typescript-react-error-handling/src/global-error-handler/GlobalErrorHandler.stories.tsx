/**
 * Imports
 */

import { Button } from "@neckar-hub/typescript-react-ui/actions";
import type { ApplicationError } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import axios from "axios";
import { http, HttpResponse } from "msw";
import { GlobalErrorHandler } from "./GlobalErrorHandler";

/**
 * Meta Information
 */

const meta = {
  title: "Error Handling / Global Error Handler",
  beforeEach: ({ msw }) => {
    msw.use(
      http.get("/network-error", () => {
        return HttpResponse.error();
      }),
      http.get("/client-version-mismatch", () => {
        const body: ApplicationError = {
          type: "ExpectedError",
          fallbackMessage: "Client Version Mismatch",
          errorCode: "PVE-100",
          instanceUri: undefined,
          attributes: undefined,
        };

        return HttpResponse.json(body, {
          status: 400,
        });
      }),
      http.get("/expected-application-error", () => {
        const body: ApplicationError = {
          type: "ExpectedError",
          fallbackMessage: "Expected Application Error",
          errorCode: "DB-100",
          instanceUri: undefined,
          attributes: undefined,
        };

        return HttpResponse.json(body, {
          status: 400,
        });
      }),
      http.get("/unexpected-application-error", () => {
        const body: ApplicationError = {
          type: "UnexpectedError",
          fallbackMessage: "Unexpected Application Error",
          exceptionType: "DB",
          instanceUri: undefined,
        };

        return HttpResponse.json(body, {
          status: 400,
        });
      }),
    );
  },
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const GlobalErrorHandlerStory: StoryObj<typeof meta> = {
  name: "GlobalErrorHandler",
  render: () => {
    const throwError = () => {
      throw new Error("Error from Button onClick");
    };

    const throwUnhandledReception = async () => {
      await new Promise((resolve, reject) => {
        reject(new Error("Unhandled Rejection from Button onClick"));
      });
    };

    const requestWithNetworkError = async () => {
      await axios.get("/network-error");
    };

    const requestWithClientVersionMismatch = async () => {
      await axios.get("/client-version-mismatch");
    };

    const requestWithExpectedApplicationError = async () => {
      await axios.get("/expected-application-error");
    };

    const requestWithUnexpectedApplicationError = async () => {
      await axios.get("/unexpected-application-error");
    };

    return (
      <GlobalErrorHandler
        contactEmail="support@example.com"
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
      >
        <div className="flex flex-col items-center gap-4">
          <Button onClick={throwError}>Throw Error</Button>
          <Button onClick={throwUnhandledReception}>Throw unhandled Reception</Button>
          <Button onClick={requestWithNetworkError}>Request with Network Error</Button>
          <Button onClick={requestWithClientVersionMismatch}>Request with Client Version Mismatch</Button>
          <Button onClick={requestWithExpectedApplicationError}>Request with Expected Application Error</Button>
          <Button onClick={requestWithUnexpectedApplicationError}>Request with Unexpected Application Error</Button>
        </div>
      </GlobalErrorHandler>
    );
  },
};
