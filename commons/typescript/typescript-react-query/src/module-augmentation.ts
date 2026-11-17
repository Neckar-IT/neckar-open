import "@tanstack/react-query";

declare module "@tanstack/react-query" {
  interface Register {
    mutationMeta: {
      /**
       * Skips the reset of all queries after this mutation succeeds.
       */
      skipGlobalQueryResets?: boolean;
      /**
       * `bad/*` response types that reset all queries like a success, e.g. `["bad/NotFound"]` for a delete whose
       * record is gone either way.
       */
      resetQueriesOnBadResponses?: readonly string[];
    };
    queryMeta: {
      /**
       * Keeps this query out of the reset of all queries after a successful mutation.
       */
      preventBeingResetByMutations?: boolean;
      /**
       * A descriptive label or i18n key for the entity being queried (e.g., "Zusatzinstallation").
       * When set, `BadResponseFallback` uses this to show a precise NotFound message
       * like "Zusatzinstallation nicht gefunden" instead of the generic "Nichts gefunden!".
       */
      entityLabel?: string;
    };
  }
}
