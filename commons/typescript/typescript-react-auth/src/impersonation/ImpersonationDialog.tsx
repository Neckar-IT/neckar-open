/**
 * Imports
 */

import { type QueryResult, QueryStatus, useQueryForSearchResults } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { Combobox } from "@neckar-hub/typescript-react-ui/inputs";
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@neckar-hub/typescript-react-ui/overlays";
import type { ImpersonationCandidate, ImpersonationCandidatesResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { type ReactElement, useState } from "react";
import { formatUserName } from "../components/formatUserName";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { startImpersonation } from "./impersonationStorage";

/** How many candidates one page of the picker holds; the search reaches the rest. */
const ImpersonationCandidatesPageSize = 20;

/**
 * Prop Types
 */

/**
 * The query parameters of `GET /api/admin/impersonation-candidates`, so the product's generated
 * client function fits [ImpersonationDialogProps.onSearchCandidates] as it is.
 */
interface ImpersonationCandidatesSearch {
  freeText?: string;
  pageSize: number;
}

interface ImpersonationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Loads the users the signed-in user may impersonate, usually the product's generated `getImpersonationCandidates`. */
  onSearchCandidates: (search: ImpersonationCandidatesSearch) => Promise<ImpersonationCandidatesResponse>;
}

/**
 * Lets a user holding `core:impersonate-users` pick the user this tab acts as. The search only runs while the dialog is open,
 * because the picker below mounts with the dialog content.
 */
export function ImpersonationDialog({ open, onOpenChange, onSearchCandidates }: ImpersonationDialogProps): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("impersonation.title")}</DialogTitle>
          <DialogDescription>{t("impersonation.description")}</DialogDescription>
        </DialogHeader>
        <ImpersonationPicker onSearchCandidates={onSearchCandidates} />
      </DialogContent>
    </Dialog>
  );
}

function ImpersonationPicker({ onSearchCandidates }: Pick<ImpersonationDialogProps, "onSearchCandidates">): ReactElement {
  const { t } = useEmbeddedTranslation();
  const [freeText, setFreeText] = useState("");
  const [selectedCandidate, setSelectedCandidate] = useState<ImpersonationCandidate | undefined>(undefined);
  const candidatesQuery = useQueryForSearchResults({
    queryKey: ["impersonation-candidates", freeText],
    queryFn: () => onSearchCandidates({ freeText: freeText === "" ? undefined : freeText, pageSize: ImpersonationCandidatesPageSize }),
  });
  const page = loadedPage(candidatesQuery);

  return (
    <>
      <DialogBody>
        <Combobox
          bottomOfTheList={
            page.matchCount > page.candidates.length ? (
              <div className="text-muted-foreground border-t py-2 text-center text-sm">{t("impersonation.narrowSearch")}</div>
            ) : undefined
          }
          filter={freeText}
          itemRenderer={(candidate) => <ImpersonationCandidateLabel candidate={candidate} />}
          items={page.candidates}
          selectedItem={selectedCandidate}
          setSelectedItem={setSelectedCandidate}
          // The combobox translates in the namespace of the app and its own; the prefix names this package's namespace.
          textKeys={{ placeholder: "auth:impersonation.search", search: "auth:impersonation.search", empty: "auth:impersonation.empty" }}
          variant="flat"
          onFilterChange={setFreeText}
        />
      </DialogBody>

      <DialogFooter>
        <DialogClose asChild>
          <Button variant="outline">{t("impersonation.cancel")}</Button>
        </DialogClose>
        <Button
          disabled={selectedCandidate === undefined}
          onClick={() => {
            if (selectedCandidate !== undefined) {
              startImpersonation(selectedCandidate.userId);
            }
          }}
        >
          {t("impersonation.start")}
        </Button>
      </DialogFooter>
    </>
  );
}

/** The loaded page plus how many candidates match in total, so the list can say that it is cut off. */
function loadedPage(candidatesQuery: QueryResult<ImpersonationCandidatesResponse>): {
  candidates: ImpersonationCandidate[];
  matchCount: number;
} {
  switch (candidatesQuery.status) {
    case QueryStatus.Pending:
      return { candidates: [], matchCount: 0 };
    case QueryStatus.Fetching:
    case QueryStatus.Success:
      return { candidates: candidatesQuery.response.data, matchCount: candidatesQuery.response.paginationInfo.totalElementsCount };
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(candidatesQuery)}`);
  }
}

function ImpersonationCandidateLabel({ candidate }: { candidate: ImpersonationCandidate }): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <span>
      <span className="block">{formatUserName(candidate) || candidate.email}</span>
      <span className="text-muted-foreground block text-xs">
        {candidate.email} · {candidate.tenantName ?? t("impersonation.withoutTenant")}
      </span>
    </span>
  );
}
