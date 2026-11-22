/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { Alert, AlertDescription, AlertTitle } from "@neckar-hub/typescript-react-ui/feedback";
import { Form, PasswordInputFormField, TextInputFormField } from "@neckar-hub/typescript-react-ui/forms";
import { FullScreenCenter, useDocumentTitle, Card, CardContent, CardHeader, CardTitle } from "@neckar-hub/typescript-react-ui/layout";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { LoginRequest, LoginResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { AlertCircleIcon } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useLoginMutation } from "../hooks/useLoginMutation";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

/**
 * TextKeys for the Login Screen
 */

export interface LoginScreenTextKeys {
  /** Document title of the screen — the app name is appended automatically. */
  htmlTitle?: TextKey;
  signIn?: TextKey;
  userName?: TextKey;
  userNameEmpty?: TextKey;
  password?: TextKey;
  passwordEmpty?: TextKey;
  errorHeading?: TextKey;
  invalidCredentials?: TextKey;
  userDisabled?: TextKey;
}

/**
 * Prop Types
 */

interface LoginScreenProps {
  header?: React.ReactNode;
  footer?: React.ReactNode;
  onLogin: (loginRequest: LoginRequest) => Promise<LoginResponse>;
  textKeys?: LoginScreenTextKeys;
}

export function LoginScreen(props: LoginScreenProps): React.ReactElement {
  const textKeys = {
    htmlTitle: props.textKeys?.htmlTitle ?? "loginScreen.htmlTitle",
    signIn: props.textKeys?.signIn ?? "loginScreen.signIn",
    userName: props.textKeys?.userName ?? "loginScreen.userName",
    userNameEmpty: props.textKeys?.userNameEmpty ?? "loginScreen.userNameEmpty",
    password: props.textKeys?.password ?? "loginScreen.password",
    passwordEmpty: props.textKeys?.passwordEmpty ?? "loginScreen.passwordEmpty",
    errorHeading: props.textKeys?.errorHeading ?? "loginScreen.errorHeading",
    invalidCredentials: props.textKeys?.invalidCredentials ?? "loginScreen.invalidCredentials",
    userDisabled: props.textKeys?.userDisabled ?? "loginScreen.userDisabled",
  };

  const { t } = useEmbeddedTranslation();
  const [errorMessage, setErrorMessage] = useState<string>("");

  useDocumentTitle(t(textKeys.htmlTitle));

  const formSchema = z.object({
    loginName: z.string().min(1, t(textKeys.userNameEmpty)),
    password: z.string().min(1, t(textKeys.passwordEmpty)),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      loginName: "",
      password: "",
    },
  });

  const { mutateAsync: login } = useLoginMutation(props.onLogin);
  const handleSubmit = form.handleSubmit(async (values) => {
    const response = await login({ loginName: values.loginName, password: values.password });

    switch (response.type) {
      case "good":
        setErrorMessage("");
        break;
      case "bad/InvalidCredentials":
        setErrorMessage(t(textKeys.invalidCredentials));
        break;
      case "bad/UserDisabled":
        setErrorMessage(t(textKeys.userDisabled));
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: response.type`);
    }
  });

  return (
    <FullScreenCenter data-testid="LoginScreen">
      <div className="w-full max-w-lg space-y-4">
        {isDefined(props.header) ? <div>{props.header}</div> : null}
        <Card>
          <CardHeader>
            <CardTitle>{t(textKeys.signIn)}</CardTitle>
          </CardHeader>
          <CardContent>
            {errorMessage ? (
              <Alert className="mb-4" variant="destructive">
                <AlertCircleIcon className="h-4 w-4" />
                <AlertTitle>{t(textKeys.errorHeading)}</AlertTitle>
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
            ) : null}

            <Form {...form} onSubmit={handleSubmit}>
              <TextInputFormField formSchema={formSchema} label={t(textKeys.userName)} fieldName="loginName" autoComplete="username" />
              <PasswordInputFormField className="mt-4" formSchema={formSchema} label={t(textKeys.password)} fieldName="password" />
              <Button className="mt-4 w-full" loading={form.formState.isSubmitting} type="submit">
                {t(textKeys.signIn)}
              </Button>
            </Form>
          </CardContent>
        </Card>
        {isDefined(props.footer) ? <div>{props.footer}</div> : null}
      </div>
    </FullScreenCenter>
  );
}
