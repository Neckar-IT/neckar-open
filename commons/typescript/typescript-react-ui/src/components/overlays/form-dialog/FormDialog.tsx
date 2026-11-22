/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { useId } from "react";
import { useForm, type DefaultValues, type Resolver, type UseFormReturn } from "react-hook-form";
import type { ZodObject, z } from "zod";
import { useActionLock } from "../../actions/ActionLockProvider";
import { UnknownOutcomeAlert } from "../../feedback/unknown-outcome";
import { useSubmittingShell } from "../../feedback/unknown-outcome/unknownOutcomeStore";
import { Form } from "../../forms/form";
import { SaveDialog, type SaveDialogProps } from "../save-dialog";

/**
 * Prop Types
 */

interface FormDialogProps<T extends ZodObject> extends Omit<SaveDialogProps, "onSubmit"> {
  formSchema: T;
  defaultValues?: DefaultValues<z.input<T>>;
  onSubmit: (values: z.output<T>, form: UseFormReturn<z.input<T>, unknown, z.infer<T>>) => void | Promise<void>;
}

/**
 * Dialog that wraps a form with a cancel and submit button.
 * Shows `UnknownOutcomeAlert` when the outcome of the submit is unknown.
 */

export function FormDialog<T extends ZodObject>(props: FormDialogProps<T>): React.ReactElement {
  const { formSchema, defaultValues, onSubmit, onClose, disabledSubmission, isPending, ...rest } = props;

  /**
   * mode: "onSubmit"
   * revalidateMode: "onChange"
   *
   * These settings are the default from react-hook-form. The first validation is triggered when the user tries to
   * submit the form. After that validation is triggered on every change for the fields with errors.
   */
  const form = useForm<z.input<T>, unknown, z.infer<T>>({
    mode: "onChange",
    reValidateMode: "onChange",
    // zodResolver cannot infer the concrete input/output types through the generic ZodObject `T`
    // (it widens to Record<string, unknown>), so the resolver is cast to the types useForm expects.
    resolver: zodResolver(formSchema) as unknown as Resolver<z.input<T>, unknown, z.infer<T>>,
    defaultValues,
  });

  const handleSubmit = form.handleSubmit((values) => onSubmit(values, form));
  const shellId = useId();
  useSubmittingShell(shellId, form.formState.isSubmitting);
  const isLocked = useActionLock() !== undefined;

  const handleClose = () => {
    form.reset();
    if (onClose) {
      onClose();
    }
  };

  const closeForm = () => {
    handleClose();
    rest.onOpenChange(false);
  };

  return (
    <SaveDialog
      {...rest}
      isPending={isPending || form.formState.isSubmitting}
      onClose={handleClose}
      onSubmit={handleSubmit}
      disabledSubmission={disabledSubmission}
    >
      <Form {...form}>{props.children}</Form>
      <UnknownOutcomeAlert
        shellId={shellId}
        onSendAgain={() => void handleSubmit()}
        sendAgainDisabled={(disabledSubmission ?? false) || (isPending ?? false) || isLocked}
        onCloseForm={closeForm}
      />
    </SaveDialog>
  );
}
