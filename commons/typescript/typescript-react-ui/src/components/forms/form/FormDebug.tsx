import { isDevelopmentStage } from "@neckar-hub/typescript-utils/runtime-context";
import { type FieldErrors, useFormContext } from "react-hook-form";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../primitives/tabs";

/**
 * Enables the React Hook Form DevTools for debugging forms.
 *
 * Gated on the deployment stage resolved at serve time, not on `import.meta.env.DEV`. The latter is
 * a build-time constant that is false in every built image, so the panel was missing on exactly the
 * deployments one debugs against — dev.<product>.neckar.it — and only ever showed up in a local
 * `pnpm dev`. The stage comes from the container environment, so one image stays servable on every
 * stage and the panel appears wherever that stage is Development.
 *
 * This costs no bundle size: [Form] renders this component unconditionally, so its code ships
 * either way — only the display was ever gated.
 */
export function FormDebug(): React.ReactNode {
  const ctx = useFormContext();

  if (ctx === null) {
    throw new Error("FormDebug must be used within a FormProvider or useFormContext");
  }

  if (!isDevelopmentStage()) {
    return null;
  }

  return <FormValidationStateDebug />;
}

export function FormValidationStateDebug(): React.ReactNode {
  const form = useFormContext();

  const errors = form.formState.errors;
  const errorList = flattenErrors(errors);

  return (
    <Popover modal={false}>
      <PopoverTrigger asChild>
        <button
          type="button"
          className="fixed top-3 right-3 z-9999 h-10 w-10 rounded-full bg-gray-200 text-gray-700 shadow hover:bg-gray-300"
          aria-label="Show validation debug info"
        >
          🐞
        </button>
      </PopoverTrigger>

      <PopoverContent
        preventCloseOnClickOutside={true}
        preventCloseOnFocusOutside={true}
        side="left"
        align="end"
        className="w-[520px] max-w-none space-y-3 rounded-md border bg-white p-3 text-sm text-gray-900 shadow-lg"
      >
        {/* short summary */}
        {errorList.length > 0 ? (
          <ul className="divide-y divide-gray-200">
            {errorList.map((err) => {
              const [path, message] = err.split(": ");
              return (
                <li key={err} className="flex items-baseline justify-between gap-4 py-1">
                  <span className="max-w-60 overflow-hidden font-mono text-ellipsis whitespace-nowrap text-gray-700" title={path}>
                    {path}
                  </span>
                  <span className="text-right whitespace-nowrap text-red-600">{message}</span>
                </li>
              );
            })}
          </ul>
        ) : (
          <div className="text-sm text-green-600">No validation errors</div>
        )}

        <div className="flex items-center justify-between text-xs text-gray-500">
          <span>isSubmitting: {String(form.formState.isSubmitting)}</span>
          <span>isValid: {String(form.formState.isValid)}</span>
        </div>

        <Tabs defaultValue="errors" className="flex flex-col gap-2">
          <TabsList className="inline-flex gap-2 border-b border-gray-200 pb-1">
            <TabsTrigger
              value="errors"
              className="rounded px-2 py-1 text-xs data-[state=active]:bg-gray-100 data-[state=active]:text-gray-900"
            >
              Errors
            </TabsTrigger>
            <TabsTrigger
              value="values"
              className="rounded px-2 py-1 text-xs data-[state=active]:bg-gray-100 data-[state=active]:text-gray-900"
            >
              Values
            </TabsTrigger>
            <TabsTrigger
              value="defaultValues"
              className="rounded px-2 py-1 text-xs data-[state=active]:bg-gray-100 data-[state=active]:text-gray-900"
            >
              Default Values
            </TabsTrigger>
            <TabsTrigger
              value="dirtyFields"
              className="rounded px-2 py-1 text-xs data-[state=active]:bg-gray-100 data-[state=active]:text-gray-900"
            >
              Dirty Fields
            </TabsTrigger>
            <TabsTrigger
              value="state"
              className="rounded px-2 py-1 text-xs data-[state=active]:bg-gray-100 data-[state=active]:text-gray-900"
            >
              State
            </TabsTrigger>
          </TabsList>

          <TabsContent value="errors" className="rounded border border-gray-100 bg-gray-50 p-2">
            <pre className="max-h-[220px] overflow-auto text-xs whitespace-pre-wrap">{JSON.stringify(form.formState.errors, null, 2)}</pre>
          </TabsContent>

          <TabsContent value="values" className="rounded border border-gray-100 bg-white p-2">
            <pre className="max-h-[220px] overflow-auto text-xs whitespace-pre-wrap">{JSON.stringify(form.getValues(), null, 2)}</pre>
          </TabsContent>

          <TabsContent value="defaultValues" className="rounded border border-gray-100 bg-white p-2">
            <pre className="max-h-[220px] overflow-auto text-xs whitespace-pre-wrap">
              {JSON.stringify(form.formState.defaultValues, null, 2)}
            </pre>
          </TabsContent>

          <TabsContent value="dirtyFields" className="rounded border border-gray-100 bg-white p-2">
            <pre className="max-h-[220px] overflow-auto text-xs whitespace-pre-wrap">
              {JSON.stringify(form.formState.dirtyFields, null, 2)}
            </pre>
          </TabsContent>

          <TabsContent value="state" className="rounded border border-gray-100 bg-white p-2">
            <ul className="space-y-0.5">
              <li>
                <strong>isDirty:</strong> {String(form.formState.isDirty)}
              </li>
              <li>
                <strong>isValid:</strong> {String(form.formState.isValid)}
              </li>
              <li>
                <strong>isSubmitting:</strong> {String(form.formState.isSubmitting)}
              </li>
              <li>
                <strong>isSubmitted:</strong> {String(form.formState.isSubmitted)}
              </li>
              <li>
                <strong>isSubmitSuccessful:</strong> {String(form.formState.isSubmitSuccessful)}
              </li>
              <li>
                <strong>isLoading:</strong> {String(form.formState.isLoading)}
              </li>
              <li>
                <strong>submitCount:</strong> {form.formState.submitCount}
              </li>
            </ul>
          </TabsContent>
        </Tabs>
      </PopoverContent>
    </Popover>
  );
}

function flattenErrors(errors: FieldErrors, prefix = ""): string[] {
  const result: string[] = [];
  for (const key in errors) {
    const err = errors[key];
    if (!err) continue;

    const path = prefix ? `${prefix}.${key}` : key;

    const message = err.message;
    if (message !== undefined) {
      const messageAsString = typeof message === "string" ? message : JSON.stringify(message);
      result.push(`${path}: ${messageAsString}`);
    }

    if (typeof err === "object" && !("ref" in err)) {
      const child = err;
      if (typeof child === "object") {
        result.push(...flattenErrors(child as FieldErrors, path));
      }
    }
  }
  return result;
}
