import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { parseAsString, useQueryStates } from "nuqs";
import { useMemo } from "react";
import { usePaginationSearchParams } from "../search-params/usePaginationSearchParams";
import { useSortingSearchParams } from "../search-params/useSortingSearchParams";
import type { DataSliceController, DataSliceParams, SortCriteriaOf } from "./DataSliceController";
import type { ParamCodec, ParamDefinition } from "./paramCodecs";
import { freeTextTokensOf, requiredCodec } from "./paramValues";

/**
 * The controller of a table page whose filters, pagination and sorting live in the URL, each parameter under
 * `${prefix}${Param}`. Every change of a filter goes back to the first page.
 *
 * @param definition - One codec per filter parameter of `Params`
 * @param sortCriteria - The generated const object of the sort criteria
 * @param prefix - Separates the parameters of two tables on one page
 * @param defaultPageSize - The page size while the URL names none
 */
export function useDataSliceControllerFromUrl<Params extends DataSliceParams<string>>(
  definition: ParamDefinition<Params>,
  sortCriteria: Readonly<Record<string, SortCriteriaOf<Params>>>,
  prefix?: string,
  defaultPageSize?: number,
): DataSliceController<Params, SortCriteriaOf<Params>> {
  const [pagination, setPagination, resetPageIndex] = usePaginationSearchParams(defaultPageSize, prefix);
  const [sorting, setSorting] = useSortingSearchParams(sortCriteria, prefix);

  const codecs: readonly (readonly [string, ParamCodec<unknown>])[] = useMemo(
    () => Object.entries<ParamCodec<unknown>>(definition),
    [definition],
  );
  const [urlValues, setUrlValues] = useQueryStates(Object.fromEntries(codecs.map(([name]) => [name, parseAsString])), {
    history: "push",
    urlKeys: Object.fromEntries(codecs.map(([name]) => [name, prefixedUrlKey(name, prefix)])),
  });

  const values: Record<string, unknown> = Object.fromEntries(codecs.map(([name, codec]) => [name, codec.fromUrl(urlValues[name])]));
  const setParams = (newValues: Readonly<Record<string, unknown>>) => {
    const codecOf = new Map(codecs);
    void setUrlValues(
      Object.fromEntries(Object.entries(newValues).map(([name, value]) => [name, requiredCodec(codecOf, name).toUrl(value)])),
    );
    resetPageIndex();
  };

  return {
    params: { ...pagination, ...sorting, ...values } as Params,
    freeTextTokens: freeTextTokensOf(values),
    setParam: (key, value) => setParams({ [key]: value }),
    setParams,
    setPagination,
    setSorting,
  };
}

function prefixedUrlKey(paramName: string, prefix: string | undefined): string {
  return prefix === undefined ? paramName : `${prefix}${capitalizeFirstCharacter(paramName)}`;
}
