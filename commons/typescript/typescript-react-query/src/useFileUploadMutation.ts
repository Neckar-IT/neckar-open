/**
 * Imports
 */

import { type MutationMeta, type UseMutationResult, useMutation } from "@tanstack/react-query";
import type { AxiosProgressEvent } from "axios";
import { useRef, useState } from "react";

/**
 * Uploads one file. Pass `signal` and `onUploadProgress` on to the request config (e.g. of an orval generated axios function).
 */
export type FileUploadFunction<TData> = (
  file: File,
  config: { signal: AbortSignal; onUploadProgress: (event: AxiosProgressEvent) => void },
) => Promise<TData>;

export interface UseFileUploadMutationOptions<TData> {
  upload: FileUploadFunction<TData>;
  onSuccess?: (data: TData, file: File) => void;
  meta?: MutationMeta;
}

/**
 * Phase of an upload, in the order `idle`, `indeterminate`, `uploading`, `processing`, `idle`.
 * A progress bar shows `uploading` with its fraction and `indeterminate` and `processing` without one.
 */
export type UploadProgress =
  /**
   * No upload runs: before `mutate`, and after the upload succeeded, failed or was aborted.
   */
  | { kind: "idle" }
  /**
   * The request body is being sent and no event has carried `progress` yet.
   * Stays until `processing` or `idle` if the body length is unknown, because then no event ever carries `progress`.
   */
  | { kind: "indeterminate" }
  /**
   * The request body is being sent. `fraction` is `0..<1` in whole percent; `1` is reported as `processing`.
   */
  | { kind: "uploading"; fraction: number }
  /**
   * The last byte of the request body is sent and the response is pending, e.g. while the server processes the file.
   * A buffering proxy can report this before the server has received the body.
   */
  | { kind: "processing" };

const idle: UploadProgress = { kind: "idle" };

export type UseFileUploadMutationResult<TData> = UseMutationResult<TData, Error, File> & {
  progress: UploadProgress;
  /**
   * Aborts the running upload; the mutation ends in `isError`.
   */
  abort: () => void;
};

/**
 * Mutation for the upload of a single file that reports the upload progress.
 * One instance carries one upload; parallel uploads need one instance each.
 */
export function useFileUploadMutation<TData>(options: UseFileUploadMutationOptions<TData>): UseFileUploadMutationResult<TData> {
  const [progress, setProgress] = useState<UploadProgress>(idle);
  const abortControllerRef = useRef<AbortController | null>(null);

  const mutation = useMutation<TData, Error, File>({
    mutationFn: (file) => {
      const abortController = new AbortController();
      abortControllerRef.current = abortController;

      return options.upload(file, {
        signal: abortController.signal,
        onUploadProgress: (event) => {
          if (event.progress === undefined) return;
          setProgress(
            event.progress >= 1 ? { kind: "processing" } : { kind: "uploading", fraction: Math.floor(event.progress * 100) / 100 },
          );
        },
      });
    },
    retry: 0,
    meta: options.meta,
    onMutate: () => {
      setProgress({ kind: "indeterminate" });
    },
    onSuccess: (data, file) => {
      options.onSuccess?.(data, file);
    },
    onSettled: () => {
      abortControllerRef.current = null;
      setProgress(idle);
    },
  });

  return {
    ...mutation,
    progress,
    abort: () => {
      abortControllerRef.current?.abort();
    },
  };
}
