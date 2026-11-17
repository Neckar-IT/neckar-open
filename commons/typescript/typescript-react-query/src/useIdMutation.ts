import type { Uuid } from "@neckar-hub/typescript-utils/types";
import { useMutation, type UseMutationResult } from "@tanstack/react-query";

/**
 * Mutation that calls `fn` with the id alone; tanstack's mutation context never reaches a second parameter of `fn`.
 */
export function useIdMutation<R>(fn: (id: Uuid) => Promise<R>): UseMutationResult<R, Error, Uuid> {
  return useMutation({
    mutationFn: async (id: Uuid) => fn(id),
  });
}
