import type { DataSliceController, DataSliceParams, SortCriteriaOf } from "./DataSliceController";
import type { ParamDefinition } from "./paramCodecs";
import { useDataSliceControllerFromState } from "./useDataSliceControllerFromState";
import { useDataSliceControllerFromUrl } from "./useDataSliceControllerFromUrl";

export interface DataSliceControllerHooks<Params extends DataSliceParams<string>> {
  useFromUrl: (prefix?: string) => DataSliceController<Params, SortCriteriaOf<Params>>;
  useFromState: () => DataSliceController<Params, SortCriteriaOf<Params>>;
}

/**
 * Builds the URL and the state controller of one table from one parameter definition. Call it once per module, so
 * `definition` keeps its identity across renders.
 */
export function createDataSliceControllerHooks<Params extends DataSliceParams<string>>(
  definition: ParamDefinition<Params>,
  sortCriteria: Readonly<Record<string, SortCriteriaOf<Params>>>,
): DataSliceControllerHooks<Params> {
  return {
    useFromUrl: (prefix) => useDataSliceControllerFromUrl<Params>(definition, sortCriteria, prefix),
    useFromState: () => useDataSliceControllerFromState<Params>(definition),
  };
}
