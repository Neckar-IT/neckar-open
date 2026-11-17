import type { QueryKey } from "@tanstack/react-query";

export interface EntityQueryKeys {
  all: QueryKey;
  list: (searchParams: unknown) => QueryKey;
  byId: (id: string) => QueryKey;
  preview: (id: string) => QueryKey;
  template: (...prefill: unknown[]) => QueryKey;
  check: (rule: string, value: unknown, excludeId?: string) => QueryKey;
  of: (id: string, resource: string, ...rest: unknown[]) => QueryKey;
}

/**
 * Every key starts with `entities`; `byId(id)` prefixes `preview(id)` and `of(id, …)`, so invalidating an entity
 * invalidates its sub-resources.
 */
export function entityQueryKeys(entities: string): EntityQueryKeys {
  return {
    all: [entities],
    list: (searchParams) => [entities, searchParams],
    byId: (id) => [entities, id],
    preview: (id) => [entities, id, "preview"],
    template: (...prefill) => [entities, "new", ...prefill],
    check: (rule, value, excludeId) => [entities, "check", rule, value, excludeId],
    of: (id, resource, ...rest) => [entities, id, resource, ...rest],
  };
}
