/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import { useMutation } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { Form, PasswordInputFormField, TextInputFormField } from "@neckar-hub/typescript-react-ui/forms";
import {
  FullScreenCenter,
  useDocumentTitle,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@neckar-hub/typescript-react-ui/layout";
import { setTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { RegisterRequest, RegisterResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { UserEnvironment } from "@neckar-hub/typescript-utils/openapi-types";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

/**
 * TextKeys for the Registration Screen
 */

export interface RegistrationScreenTextKeys {
  /** Document title of the screen — the app name is appended automatically. */
  htmlTitle?: TextKey;
  heading?: TextKey;
  description?: TextKey;
  descriptionEmphasis?: TextKey;
  companyName?: TextKey;
  personalData?: TextKey;
  firstName?: TextKey;
  lastName?: TextKey;
  credentials?: TextKey;
  email?: TextKey;
  emailAlreadyRegistered?: TextKey;
  password?: TextKey;
  passwordTooShort?: TextKey;
  passwordsDoNotMatch?: TextKey;
  passwordConfirmation?: TextKey;
  submit?: TextKey;
}

/**
 * Prop Types
 */

export interface RegistrationScreenProps {
  header?: React.ReactNode;
  onRegister: (data: RegisterRequest) => Promise<RegisterResponse>;
  // Called after a successful registration, before navigating away. Awaited so a product can flush
  // analytics before the full-page reload.
  onRegisterSuccess?: () => void | Promise<void>;
  textKeys?: RegistrationScreenTextKeys;
}

/**
 * RegistrationScreen Component
 */

export function RegistrationScreen(props: RegistrationScreenProps): React.ReactElement {
  const textKeys = {
    htmlTitle: props.textKeys?.htmlTitle ?? "registrationScreen.htmlTitle",
    heading: props.textKeys?.heading ?? "registrationScreen.heading",
    description: props.textKeys?.description ?? "registrationScreen.description",
    descriptionEmphasis: props.textKeys?.descriptionEmphasis ?? "registrationScreen.descriptionEmphasis",
    companyName: props.textKeys?.companyName ?? "registrationScreen.companyName",
    personalData: props.textKeys?.personalData ?? "registrationScreen.personalData",
    firstName: props.textKeys?.firstName ?? "registrationScreen.firstName",
    lastName: props.textKeys?.lastName ?? "registrationScreen.lastName",
    credentials: props.textKeys?.credentials ?? "registrationScreen.credentials",
    email: props.textKeys?.email ?? "registrationScreen.email",
    emailAlreadyRegistered: props.textKeys?.emailAlreadyRegistered ?? "registrationScreen.emailAlreadyRegistered",
    password: props.textKeys?.password ?? "registrationScreen.password",
    passwordTooShort: props.textKeys?.passwordTooShort ?? "registrationScreen.passwordTooShort",
    passwordsDoNotMatch: props.textKeys?.passwordsDoNotMatch ?? "registrationScreen.passwordsDoNotMatch",
    passwordConfirmation: props.textKeys?.passwordConfirmation ?? "registrationScreen.passwordConfirmation",
    submitButton: props.textKeys?.submit ?? "registrationScreen.submit",
  };

  const { t } = useEmbeddedTranslation();

  useDocumentTitle(t(textKeys.htmlTitle));

  const registrationFormSchema = zod
    .object({
      companyName: zod.string().nonempty(),
      firstName: zod.string().nonempty(),
      lastName: zod.string().nonempty(),
      email: zod.email(),
      password: zod.string().min(8, t(textKeys.passwordTooShort)),
      passwordConfirmation: zod.string(),
      environment: zod.array(zod.enum([UserEnvironment.PvPlanner, UserEnvironment.ElektroPlanner])), // Placeholder for future environment selection
    })
    .superRefine((values, context) => {
      if (values.password !== values.passwordConfirmation) {
        context.addIssue({
          code: "custom",
          message: t(textKeys.passwordsDoNotMatch),
          path: ["passwordConfirmation"],
        });
      }
    });

  type RegistrationFormValues = zod.infer<typeof registrationFormSchema>;

  const { mutateAsync } = useMutation({
    mutationFn: props.onRegister,
  });

  const form = useForm<RegistrationFormValues>({
    mode: "all",
    resolver: zodResolver(registrationFormSchema),
    defaultValues: {
      companyName: "",
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      environment: [],
      passwordConfirmation: "",
    },
  });

  const handleSubmit = form.handleSubmit(async (values) => {
    const response = await mutateAsync({
      companyName: values.companyName,
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      password: values.password,
      environment: values.environment,
      // Password confirmation is not sent to the server
    });

    switch (response.type) {
      case "good":
        setTokenPair(response.data.jwsTokens);
        await props.onRegisterSuccess?.();
        window.location.replace("/");
        break;
      case "bad/AlreadyRegistered":
        form.setError("email", { message: t(textKeys.emailAlreadyRegistered) });
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: response.type`);
    }
  });

  return (
    <FullScreenCenter>
      <div className="flex w-full max-w-lg flex-col gap-4">
        {isDefined(props.header) ? <div>{props.header}</div> : null}
        <Card>
          <Form {...form} onSubmit={handleSubmit}>
            <CardHeader>
              <CardTitle className="text-xl">{t(textKeys.heading)}</CardTitle>
              <CardDescription>
                {t(textKeys.description)}
                <br />
                <strong>{t(textKeys.descriptionEmphasis)}</strong>
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <TextInputFormField
                formSchema={registrationFormSchema}
                fieldName="companyName"
                label={t(textKeys.companyName)}
                autoComplete="organization"
              />
              <Divider label={t(textKeys.personalData)} />
              <TextInputFormField
                formSchema={registrationFormSchema}
                fieldName="firstName"
                label={t(textKeys.firstName)}
                autoComplete="given-name"
              />
              <TextInputFormField
                formSchema={registrationFormSchema}
                fieldName="lastName"
                label={t(textKeys.lastName)}
                autoComplete="family-name"
              />
              <TextInputFormField formSchema={registrationFormSchema} fieldName="email" label={t(textKeys.email)} autoComplete="email" />
              <Divider label={t(textKeys.credentials)} />
              <PasswordInputFormField
                formSchema={registrationFormSchema}
                fieldName="password"
                label={t(textKeys.password)}
                autoComplete="new-password"
              />
              <PasswordInputFormField
                formSchema={registrationFormSchema}
                fieldName="passwordConfirmation"
                label={t(textKeys.passwordConfirmation)}
                autoComplete="new-password"
              />
            </CardContent>
            <CardFooter>
              <Button className="w-full" type="submit" loading={form.formState.isSubmitting}>
                {t(textKeys.submitButton)}
              </Button>
            </CardFooter>
          </Form>
        </Card>
      </div>
    </FullScreenCenter>
  );
}

function Divider({ label }: { label: string }): React.ReactElement {
  return (
    <div className="after:border-border relative text-center text-sm after:absolute after:inset-0 after:top-1/2 after:z-0 after:flex after:items-center after:border-t">
      <span className="bg-card text-muted-foreground relative z-10 px-2">{label}</span>
    </div>
  );
}
