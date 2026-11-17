import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { CancelButton } from "../actions/cancel-button";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../primitives/alert-dialog";
import { Button } from "../primitives/button";

/**
 * What the user does with a save the server answered with `bad/VersionConflict`: `Reload` drops the
 * input and loads the stored state, `SaveAnyway` writes the input over it, `Cancel` leaves the form as it is.
 */
export type VersionConflictDecision = "Cancel" | "Reload" | "SaveAnyway";

/** The decisions a dialog offers beside `Cancel`, which it always offers. */
export type VersionConflictResolution = Exclude<VersionConflictDecision, "Cancel">;

/** The texts of the dialog; a key left out keeps the shared text. */
export interface VersionConflictMessages {
  title: string;
  description: string;
  reload: string;
  saveAnyway: string;
}

interface VersionConflictDialogProps {
  onDecision: (decision: VersionConflictDecision) => void;
  messages?: Partial<VersionConflictMessages>;
  /** Defaults to both; a record the user may never overwrite offers `["Reload"]`. */
  resolutions?: readonly VersionConflictResolution[];
  /** Shown between the description and the buttons, for example the fields the other save changed. */
  children?: React.ReactNode;
}

const ALL_RESOLUTIONS: readonly VersionConflictResolution[] = ["Reload", "SaveAnyway"];

/**
 * Dialog shown when a save is rejected because somebody else saved the record meanwhile; the user decides
 * whether the stored state or the own input counts.
 */
export function VersionConflictDialog({
  onDecision,
  messages,
  resolutions = ALL_RESOLUTIONS,
  children,
}: VersionConflictDialogProps): React.ReactElement {
  const { t: tEmbedded } = useEmbeddedTranslation();

  return (
    <AlertDialog
      open
      onOpenChange={(isOpen) => {
        if (!isOpen) onDecision("Cancel");
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{messages?.title ?? tEmbedded("versionConflict.title")}</AlertDialogTitle>
        </AlertDialogHeader>

        <AlertDialogDescription>{messages?.description ?? tEmbedded("versionConflict.description")}</AlertDialogDescription>

        {children}

        <AlertDialogFooter>
          <CancelButton onClick={() => onDecision("Cancel")} />
          {resolutions.includes("Reload") ? (
            <Button variant="outline" onClick={() => onDecision("Reload")}>
              {messages?.reload ?? tEmbedded("versionConflict.reload")}
            </Button>
          ) : null}
          {resolutions.includes("SaveAnyway") ? (
            <Button variant="destructive" onClick={() => onDecision("SaveAnyway")}>
              {messages?.saveAnyway ?? tEmbedded("versionConflict.saveAnyway")}
            </Button>
          ) : null}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
