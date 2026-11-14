import { InfoIcon } from "lucide-react";
import type { ReactNode } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";

interface ColumnHeaderWithHintProps {
  children: ReactNode;
  /** Opens on a click on the info icon beside the header. */
  hint: ReactNode;
}

/**
 * Column header with a hint on what the column's figures mean, such as their unit or how they are calculated.
 */
export function ColumnHeaderWithHint({ children, hint }: ColumnHeaderWithHintProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <span className="inline-flex items-center gap-1">
      {children}
      <Popover>
        <PopoverTrigger asChild>
          <Button variant="ghost" size="icon" className="text-muted-foreground size-6" aria-label={t("columnHeaderWithHint.showHint")}>
            <InfoIcon />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="text-sm font-normal">{hint}</PopoverContent>
      </Popover>
    </span>
  );
}
