/**
 * Imports
 */

import { ArrowLeftIcon } from "lucide-react";
import { useNavigate } from "react-router";
import { Button } from "../../primitives/button";

/**
 * 404 Error Page (Catch All)
 */

export function CatchAll(): React.ReactElement {
  const navigate = useNavigate();

  return (
    <div className="w.full flex h-screen items-center justify-center">
      <div className="flex w-11/12 max-w-sm flex-col items-center gap-6">
        <h1 className="text-9xl font-extralight">404</h1>
        <div className="flex flex-col items-center gap-2 text-center">
          <p className="text-lg font-semibold">Die angeforderte Seite konnte nicht gefunden werden!</p>
          <p className="text-muted-foreground text-sm">Möglicherweise sind Sie einem fehlerhaften Link gefolgt.</p>
        </div>
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeftIcon />
          <span>Zurück</span>
        </Button>
      </div>
    </div>
  );
}
