import { useState } from "react";
import type { SortingParams } from "../search-params/useSortingSearchParams";
import { usePaginationState } from "../utility-hooks/usePaginationState";
import type { DataSliceController, DataSliceParams, FilterParams, SortCriteriaOf } from "./DataSliceController";
import type { ParamCodec, ParamDefinition } from "./paramCodecs";
import { freeTextTokensOf, requiredCodec } from "./paramValues";

/**
 * The controller of a list whose state must not reach the URL, e.g. inside a select dialog opened from a form. It
 * holds the values `useDataSliceControllerFromUrl` reads from the URL; React drops them with the component.
 *
 * @param definition - One codec per filter parameter of `Params`
 * @param initialValues - Values that differ from the default value of their codec when the list opens
 * @param defaultPageSize - The page size the list opens with
 */
export function useDataSliceControllerFromState<Params extends DataSliceParams<string>>(
  definition: ParamDefinition<Params>,
  initialValues?: Partial<FilterParams<Params>>,
  defaultPageSize?: number,
): DataSliceController<Params, SortCriteriaOf<Params>> {
  const codecs = new Map(Object.entries<ParamCodec<unknown>>(definition));
  const { pagination, setPagination, withPaginationReset } = usePaginationState(defaultPageSize);
  const [sorting, setSorting] = useState<SortingParams<SortCriteriaOf<Params>> | undefined>(undefined);
  const [values, setValues] = useState<Readonly<Record<string, unknown>>>(() => ({
    ...Object.fromEntries([...codecs].map(([name, codec]) => [name, codec.defaultValue])),
    ...initialValues,
  }));

  const setParams = (newValues: Readonly<Record<string, unknown>>) => {
    for (const name of Object.keys(newValues)) {
      requiredCodec(codecs, name);
    }
    withPaginationReset(setValues)((old) => ({ ...old, ...newValues }));
  };

  return {
    params: { ...pagination, ...sorting, ...values } as Params,
    freeTextTokens: freeTextTokensOf(values),
    setParam: (key, value) => setParams({ [key]: value }),
    setParams,
    setPagination,
    setSorting: withPaginationReset(setSorting),
  };
}
