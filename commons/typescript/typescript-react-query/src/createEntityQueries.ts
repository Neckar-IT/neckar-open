import type { Uuid } from "@neckar-hub/typescript-utils/types";
import { skipToken } from "@tanstack/react-query";
import type { EntityQueryKeys } from "./entityQueryKeys";
import type { QueryResult } from "./useQuery";
import { useQueryForOneOffData } from "./useQueryForOneOffData";
import { useQueryForStaticData } from "./useQueryForStaticData";
import { useQueryForTableData } from "./useQueryForTableData";

export interface EntityQueryFunctions<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate> {
  byId: (id: Uuid) => Promise<TDetails>;
  preview: (id: Uuid) => Promise<TPreview>;
  list: (searchParams: TListParams) => Promise<TList>;
  template: (request: TTemplateRequest) => Promise<TTemplate>;
}

/**
 * The request is optional exactly when every field of `TTemplateRequest` is optional.
 */
type TemplateRequestArguments<TTemplateRequest> = {} extends TTemplateRequest ? [request?: TTemplateRequest] : [request: TTemplateRequest];

export interface EntityQueries<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate> {
  useByIdQuery: (id: Uuid | null | undefined) => QueryResult<TDetails>;
  usePreviewQuery: (id: Uuid | null | undefined) => QueryResult<TPreview>;
  useDataSliceQuery: (searchParams: NonNullable<TListParams>) => QueryResult<TList>;
  useNewQuery: (...request: TemplateRequestArguments<NonNullable<TTemplateRequest>>) => QueryResult<TTemplate>;
}

export type EntityQueriesWithoutPreview<TDetails, TListParams, TList, TTemplateRequest, TTemplate> = Omit<
  EntityQueries<TDetails, never, TListParams, TList, TTemplateRequest, TTemplate>,
  "usePreviewQuery"
>;

/**
 * Builds the four standard queries of a standalone entity: details and preview as static data, the data slice as
 * table data, the template as one-off data. A missing template request is sent and keyed as `{}`. An absent id keys
 * details and preview with `null` and skips the request, so the query stays pending.
 */
export function createEntityQueries<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>(
  keys: EntityQueryKeys,
  functions: EntityQueryFunctions<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>,
): EntityQueries<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>;
/**
 * Builds the queries of a standalone entity whose API has no preview endpoint: details, data slice and template.
 */
export function createEntityQueries<TDetails, TListParams, TList, TTemplateRequest, TTemplate>(
  keys: EntityQueryKeys,
  functions: Omit<EntityQueryFunctions<TDetails, never, TListParams, TList, TTemplateRequest, TTemplate>, "preview">,
): EntityQueriesWithoutPreview<TDetails, TListParams, TList, TTemplateRequest, TTemplate>;
export function createEntityQueries<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>(
  keys: EntityQueryKeys,
  functions: Omit<EntityQueryFunctions<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>, "preview"> &
    Partial<Pick<EntityQueryFunctions<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>, "preview">>,
): EntityQueriesWithoutPreview<TDetails, TListParams, TList, TTemplateRequest, TTemplate> &
  Partial<Pick<EntityQueries<TDetails, TPreview, TListParams, TList, TTemplateRequest, TTemplate>, "usePreviewQuery">> {
  const { preview } = functions;
  return {
    useByIdQuery: (id) =>
      useQueryForStaticData(
        id == null ? { queryKey: [...keys.all, null], queryFn: skipToken } : { queryKey: keys.byId(id), queryFn: () => functions.byId(id) },
      ),
    ...(preview === undefined
      ? {}
      : {
          usePreviewQuery: (id: Uuid | null | undefined) =>
            useQueryForStaticData(
              id == null
                ? { queryKey: [...keys.all, null, "preview"], queryFn: skipToken }
                : { queryKey: keys.preview(id), queryFn: () => preview(id) },
            ),
        }),
    useDataSliceQuery: (searchParams) =>
      useQueryForTableData({
        queryKey: keys.list(searchParams),
        queryFn: () => functions.list(searchParams),
      }),
    useNewQuery: (...[request]) => {
      // The argument type admits `undefined` only when `{}` is a valid request.
      const templateRequest = request ?? ({} as NonNullable<TTemplateRequest>);
      return useQueryForOneOffData({
        queryKey: keys.template(templateRequest),
        queryFn: () => functions.template(templateRequest),
      });
    },
  };
}
