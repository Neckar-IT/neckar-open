import { zodResolver } from "@hookform/resolvers/zod";
import { type QueryKey, useQueryClient } from "@neckar-hub/typescript-react-query";
import { type FieldPathByValue, type FieldValues, get, type Resolver, set } from "react-hook-form";
import type { ZodType } from "zod";

/**
 * `ask` and `queryKey` take the field's value and the form's own id, since a value does not collide with itself.
 * `verdict` maps the answer to the field's message, or `null` when the server accepts the value, on every resolve, so the message follows the language.
 */
export type ServerCheck<T extends FieldValues, A> = {
  field: FieldPathByValue<T, string | null | undefined>;
  queryKey: (value: string, id: string) => QueryKey;
  ask: (value: string, id: string) => Promise<A>;
  verdict: (answer: A) => string | null;
};

/**
 * Form resolver that validates with [schema] first and asks the server about [check.field] only for a filled value Zod accepts.
 * An answer is cached for 30 seconds per query key, so typing back to a checked value sends no request.
 */
export function useServerCheckedResolver<T extends FieldValues & { id: string }, A>(
  schema: ZodType<T, T>,
  check: ServerCheck<T, A>,
): Resolver<T> {
  const queryClient = useQueryClient();
  const zodResolve: Resolver<T> = zodResolver(schema);
  const { field, queryKey, ask, verdict } = check;

  return async (values, context, options) => {
    const zodResult = await zodResolve(values, context, options);
    if (get(zodResult.errors, field) !== undefined) return zodResult;

    const value: unknown = get(values, field);
    if (typeof value !== "string" || !value.trim()) return zodResult;

    const answer = await queryClient.query({
      queryKey: queryKey(value, values.id),
      queryFn: () => ask(value, values.id),
      staleTime: 30_000,
    });
    const message = verdict(answer);
    if (message === null) return zodResult;

    set(zodResult.errors, field, { type: "server", message });
    return { values: {}, errors: zodResult.errors };
  };
}
