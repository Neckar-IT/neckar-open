/**
 * Imports
 */

import { msw, type Story, type StoryDefault } from "@ladle/react";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import type { ApplicationError } from "@neckar-hub/typescript-utils/openapi-types";
import axios from "axios";
import { GlobalErrorHandler } from "./GlobalErrorHandler";

/**
 * Meta Information
 */

export default {
  title: "Error Handling",
  msw: [
    msw.http.get("/network-error", () => {
      return msw.HttpResponse.error();
    }),

    msw.http.get("/client-version-mismatch", () => {
      const body: ApplicationError = {
        type: "ExpectedError",
        fallbackMessage: "Client Version Mismatch",
        errorCode: "PVE-100",
        instanceUri: undefined,
        attributes: undefined,
      };

      return msw.HttpResponse.json(body, {
        status: 400,
      });
    }),

    msw.http.get("/expected-application-error", () => {
      const body: ApplicationError = {
        type: "ExpectedError",
        fallbackMessage: "Expected Application Error",
        errorCode: "DB-100",
        instanceUri: undefined,
        attributes: undefined,
      };

      return msw.HttpResponse.json(body, {
        status: 400,
      });
    }),

    msw.http.get("/unexpected-application-error", () => {
      const body: ApplicationError = {
        type: "UnexpectedError",
        fallbackMessage: "Unexpected Application Error",
        exceptionType: "DB",
        instanceUri: undefined,
      };

      return msw.HttpResponse.json(body, {
        status: 400,
      });
    }),
  ],
} satisfies StoryDefault;

/**
 * Stories
 */

export const GlobalErrorHandlerStory: Story = () => {
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
};
GlobalErrorHandlerStory.storyName = "GlobalErrorHandler";
