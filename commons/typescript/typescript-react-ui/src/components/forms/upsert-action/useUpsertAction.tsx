import { resetQueriesExceptPrevented, useQueryClient } from "@neckar-hub/typescript-react-query";
import { SaveIcon } from "lucide-react";
import { useRef } from "react";
import type { FieldValues, UseFormReturn } from "react-hook-form";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import type { Action } from "../../actions/Action";
import { toast, withActionFailedToast } from "../../feedback";
import { useAskVersionConflictDecision } from "../../optimistic-locking/useAskVersionConflictDecision";
import { useInvalidFormSubmitHandler } from "../form/useInvalidFormSubmitHandler";

interface VersionConflictResponse {
  type: "bad/VersionConflict";
  baseVersion: number | null;
  actualVersion: number;
}

/** The answer of an upsert endpoint, discriminated by `type`; `good` carries the saved record in `data`. */
export type UpsertResponse = { type: string };

export type SavedData<TResponse extends UpsertResponse> =
  Extract<TResponse, { type: "good" }> extends { data: infer TData } ? TData : never;

/** Every answer but `good`, `bad/VersionConflict` and `bad/NotFound`, which {@link useUpsertAction} handles itself. */
export type OtherBadResponse<TResponse extends UpsertResponse> = Exclude<
  TResponse,
  { type: "good" } | { type: "bad/VersionConflict" } | { type: "bad/NotFound" }
>;

type Answers<TResponse, TType extends string> = [Extract<TResponse, { type: TType }>] extends [never] ? false : true;

/** `alreadyCreated` answers a `bad/VersionConflict` on a creation, `notFound` answers `bad/NotFound`; each is required when the endpoint sends it. */
export type UpsertMessages<TResponse extends UpsertResponse> = { saved: string } & (Answers<TResponse, "bad/VersionConflict"> extends true
  ? { alreadyCreated: string }
  : { alreadyCreated?: never }) &
  (Answers<TResponse, "bad/NotFound"> extends true ? { notFound: string } : { notFound?: never });

/**
 * Maps every other `bad/*` answer onto the form fields or a toast; required when the endpoint sends one, optional
 * otherwise, so every upsert form can pass the mapping of its own endpoint.
 */
export type ServerErrorMapping<TResponse extends UpsertResponse> = [OtherBadResponse<TResponse>] extends [never]
  ? { mapServerErrors?: (response: never) => void }
  : { mapServerErrors: (response: OtherBadResponse<TResponse>) => void };

export type UseUpsertActionOptions<TValues extends FieldValues, TRequest, TResponse extends UpsertResponse> = {
  /** A creation sends no version; an entity without a version leaves the mode out. */
  mode?: "new" | "update";
  form: UseFormReturn<TValues>;
  /** The version the form was loaded at; absent for an entity without a version. */
  currentVersion?: number;
  mutation: (request: TRequest) => Promise<TResponse>;
  toRequest: (values: TValues, baseVersion: number | null) => TRequest;
  messages: UpsertMessages<TResponse>;
  onSaved: (data: SavedData<TResponse>) => void;
  /**
   * Runs before every save the user triggers, not before the save anyway of a version conflict; `false` sends
   * nothing, for example after a cancelled warning or a failed client-side check.
   */
  confirmBeforeSave?: (values: TValues) => Promise<boolean>;
  disabled?: boolean;
  disabledReason?: React.ReactNode;
} & ServerErrorMapping<TResponse>;

/**
 * The save action of an upsert form. A save somebody else overtook opens the version conflict dialog of
 * `<DialogRegistry />`, and the decision runs at once, so no later click overwrites unseen.
 */
export function useUpsertAction<TValues extends FieldValues, TRequest, TResponse extends UpsertResponse>(
  options: UseUpsertActionOptions<TValues, TRequest, TResponse>,
): Action {
  const { mode, form, currentVersion, mutation, toRequest, onSaved, confirmBeforeSave, disabled, disabledReason } = options;
  // The option types tie each text to the answers of TResponse; read here, they are plain optionals.
  const messages: { saved: string; alreadyCreated?: string; notFound?: string } = options.messages;
  // Without another bad answer the mapping takes `never`, which is OtherBadResponse<TResponse> resolved.
  const mapServerErrors = options.mapServerErrors as ((response: OtherBadResponse<TResponse>) => void) | undefined;
  const { t } = useEmbeddedTranslation();
  const onInvalidSubmit = useInvalidFormSubmitHandler<TValues>();
  const askVersionConflictDecision = useAskVersionConflictDecision();
  const queryClient = useQueryClient();
  // Taken when the form loads and moved only by this form's own saves: a refetch on window focus
  // would otherwise pair a newer version with values built on the old one.
  const versionToSend = useRef<number | null>(mode === "new" ? null : (currentVersion ?? null));
  const saveLabel = t("submitButton.label");

  const handleVersionConflict = async (response: VersionConflictResponse, values: TValues): Promise<void> => {
    if (response.baseVersion === null) {
      if (messages.alreadyCreated === undefined) throw new Error("bad/VersionConflict on a creation needs messages.alreadyCreated");
      // A creation took the id meanwhile: there is no stored version to overwrite.
      toast.error(messages.alreadyCreated);
      return;
    }
    const decision = await askVersionConflictDecision();
    switch (decision) {
      case "Cancel":
        // The version stays, so the next save meets the same conflict and asks again.
        return;
      case "Reload":
        void resetQueriesExceptPrevented(queryClient);
        return;
      case "SaveAnyway":
        versionToSend.current = response.actualVersion;
        // Awaited, so the surrounding handleSubmit keeps isSubmitting set until the retry is done.
        await submit(values);
        return;
      default:
        throw new Error(`Unexpected value in Switch Statement: decision`);
    }
  };

  const handleResponse = async (response: TResponse, values: TValues): Promise<void> => {
    if (isGood(response)) {
      toast.success(messages.saved);
      // Without a history entry to go back to, the form stays open and its next save builds on this one.
      versionToSend.current = currentVersionOf(response.data) ?? versionToSend.current;
      onSaved(response.data as SavedData<TResponse>);
    } else if (isVersionConflict(response)) {
      await handleVersionConflict(response, values);
    } else if (response.type === "bad/NotFound") {
      if (messages.notFound === undefined) throw new Error("bad/NotFound needs messages.notFound");
      toast.error(messages.notFound);
    } else if (mapServerErrors !== undefined) {
      mapServerErrors(response as OtherBadResponse<TResponse>);
    } else {
      throw new Error(`Unhandled upsert response: ${response.type}`);
    }
  };

  const submit = async (values: TValues): Promise<void> => {
    const response = await withActionFailedToast(saveLabel, mutation(toRequest(values, versionToSend.current)));
    await handleResponse(response, values);
  };

  const save = async (values: TValues): Promise<void> => {
    const confirmed = confirmBeforeSave === undefined || (await confirmBeforeSave(values));
    if (confirmed) await submit(values);
  };

  return {
    icon: <SaveIcon />,
    label: saveLabel,
    disabled,
    disabledReason,
    onTrigger: () => {
      void form.handleSubmit(save, onInvalidSubmit)();
    },
  };
}

function isGood(response: UpsertResponse): response is { type: "good"; data: unknown } {
  return response.type === "good";
}

function isVersionConflict(response: UpsertResponse): response is VersionConflictResponse {
  return response.type === "bad/VersionConflict";
}

function currentVersionOf(data: unknown): number | undefined {
  if (typeof data === "object" && data !== null && "currentVersion" in data && typeof data.currentVersion === "number") {
    return data.currentVersion;
  }
  return undefined;
}
