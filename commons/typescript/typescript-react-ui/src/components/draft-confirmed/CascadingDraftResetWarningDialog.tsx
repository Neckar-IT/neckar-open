import { useTranslation } from "@neckar-hub/typescript-react-i18n";
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

export interface UsedByCount {
  type: string;
  activeConfirmedCount?: number | null;
}

interface CascadingDraftResetWarningDialogProps {
  usedByCounts: UsedByCount[];
  onOpenChange: (open: boolean) => void;
  onSubmit: () => void;
}

/**
 * Warning dialog shown when editing a confirmed entity.
 * Warns the user that the confirmation status of both the entity and its dependents will be reset.
 *
 * For the entities to be listed with their correct (translated) names, you need to provide the entity type in your language files:
 * ```
 * "entities": {
 *   "Entity_one": "{{count}} Entity",
 *   "Entity_other": "{{count}} Entities"
 * }
 * ```
 */
export function CascadingDraftResetWarningDialog({
  usedByCounts,
  onOpenChange,
  onSubmit,
}: CascadingDraftResetWarningDialogProps): React.ReactElement {
  const { t } = useTranslation();
  const { t: tEmbedded } = useEmbeddedTranslation();

  const withConfirmed = usedByCounts.filter((entry) => entry.activeConfirmedCount != null && entry.activeConfirmedCount > 0);

  return (
    <AlertDialog open onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{tEmbedded("confirmation.cascadingReset.title")}</AlertDialogTitle>
          <AlertDialogDescription>{tEmbedded("confirmation.cascadingReset.description")}</AlertDialogDescription>
        </AlertDialogHeader>

        {withConfirmed.length > 0 ? (
          <div className="text-muted-foreground text-sm">
            {tEmbedded("confirmation.cascadingReset.dependenciesNote")}
            <ul className="mt-1 list-disc pl-7">
              {withConfirmed.map((entity) => (
                <li key={entity.type}>{t(`entities.${entity.type}`, { count: entity.activeConfirmedCount })}</li>
              ))}
            </ul>
          </div>
        ) : null}

        <AlertDialogFooter>
          <CancelButton onClick={() => onOpenChange(false)} />
          <Button variant="destructive" onClick={onSubmit}>
            {tEmbedded("confirmation.cascadingReset.saveAnyway")}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
