import { enumTranslationKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import {
  type DataSliceController,
  type DataSliceParams,
  type FilterParams,
  parseInstant,
  parseLocalDate,
  type SortCriteriaOf,
} from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { toLocalDate } from "@neckar-hub/typescript-utils/date";
import { ArchiveFilter, type ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { LinkIcon } from "lucide-react";
import { type ComponentType, type ReactElement, type ReactNode } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { FilterByConfirmationStatus } from "../../draft-confirmed/FilterByConfirmationStatus";
import { ArchiveScope } from "../../entities/archive-scope";
import { FilterByDateRange, type OpenDateRange } from "../../inputs/date-range";
import { FilterByDateTimeRange } from "../date-time-range";
import { FilterByBooleanInput } from "../filter-by-boolean-input";
import { FilterByEntity } from "../filter-by-entity";
import { FilterByItems } from "../filter-by-items";
import { FilterByNumberRange, type NumberRange, type NumberRangeKind, type RangeBounds } from "../filter-by-number-range";
import { FilterByTextInput } from "../filter-by-text-input";
import { FilterByYear } from "../filter-by-year";
import { FilterTrigger } from "../filter-trigger";
import type { FilterForm, TableFilter } from "./TableFilter";

/**
 * The part of a `DataSliceController` a filter reads and writes.
 */
export type FilterController<Params extends DataSliceParams<string>> = Pick<
  DataSliceController<Params, SortCriteriaOf<Params>>,
  "params" | "setParam" | "setParams"
>;

/** The names of the filter parameters whose values are of type `Value`. */
export type ParamName<Params extends DataSliceParams<string>, Value> = {
  [Key in keyof FilterParams<Params> & string]-?: [NonNullable<FilterParams<Params>[Key]>] extends [Value] ? Key : never;
}[keyof FilterParams<Params> & string];

type ParamValue<Params, Key> = Key extends keyof Params ? Params[Key] : never;

/** The type of the items of a list parameter, the type itself for any other. */
type ElementOf<Value> = NonNullable<Value> extends readonly (infer Item)[] ? Item : NonNullable<Value>;

type EnumValue<Params extends DataSliceParams<string>, Key extends string> = ElementOf<ParamValue<Params, Key>> & string;

/** The value of a filter parameter, typed by the caller who chose a parameter of that type. */
function valueOf<Value>(controller: FilterController<DataSliceParams<string>>, param: string): Value | undefined {
  return (controller.params as Record<string, unknown>)[param] as Value | undefined;
}

function setValues(controller: FilterController<DataSliceParams<string>>, values: Record<string, unknown>): void {
  (controller.setParams as (values: Record<string, unknown>) => void)(values);
}

/** The controller with the parameters erased, as the builders below read it. */
function erased<Params extends DataSliceParams<string>>(controller: FilterController<Params>): FilterController<DataSliceParams<string>> {
  return controller as unknown as FilterController<DataSliceParams<string>>;
}

/**
 * The search field over the parameter `freeText`: a field at the start of the toolbar, a plain field inside the popover.
 */
export function searchFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  { label, param = "freeText" as ParamName<Params, string> }: { label: string; param?: ParamName<Params, string> },
): TableFilter {
  const value = valueOf<string>(erased(controller), param);
  return {
    key: param,
    label,
    active: value !== undefined && value !== "",
    render: ({ label: translated, form }) => (
      <FilterByTextInput
        form={form === "Inline" ? "Search" : "Panel"}
        label={translated}
        value={value}
        onChange={(newValue) => setValues(erased(controller), { [param]: newValue })}
      />
    ),
  };
}

/** A text parameter, edited in a popover. */
export function textFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  { param, label }: { param: ParamName<Params, string>; label: string },
): TableFilter {
  const value = valueOf<string>(erased(controller), param);
  return {
    key: param,
    label,
    active: value !== undefined && value !== "",
    render: ({ label: translated, form }) => (
      <FilterByTextInput
        form={form}
        label={translated}
        value={value}
        onChange={(newValue) => setValues(erased(controller), { [param]: newValue })}
      />
    ),
  };
}

/**
 * One exact value of a text parameter, set by a link to the table and never typed — a typed part of an email address
 * would match nothing the request accepts. Shown only while set, as its value with an X.
 */
export function keyFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  { param, label }: { param: ParamName<Params, string>; label: string },
): TableFilter {
  const value = valueOf<string>(erased(controller), param);
  const active = value !== undefined && value !== "";
  return {
    key: param,
    label,
    active,
    onlyWhileSet: true,
    render: ({ label: translated }) => (
      <FilterTrigger
        icon={LinkIcon}
        label={translated}
        valueText={value}
        active={active}
        onClear={() => setValues(erased(controller), { [param]: undefined })}
      />
    ),
  };
}

interface EnumFilterOptions<Params extends DataSliceParams<string>, Key extends string> {
  param: Key;
  label: string;
  /** The generated const object of the enum, e.g. `PublicationStatus`. */
  values: Readonly<Record<string, EnumValue<Params, Key>>>;
  /** Qualifies the option labels `enums.<EnumName>.<Value>`. */
  enumName?: string;
  /** A richer rendering of an option in the list, e.g. a badge; the button keeps the translated text. */
  renderOption?: (value: EnumValue<Params, Key>) => ReactNode;
}

/** One value of an enum parameter, picked from a list. */
export function enumFilter<Params extends DataSliceParams<string>, Key extends ParamName<Params, string>>(
  controller: FilterController<Params>,
  options: EnumFilterOptions<Params, Key>,
): TableFilter {
  const value = valueOf<string>(erased(controller), options.param);
  return {
    key: options.param,
    label: options.label,
    active: value !== undefined,
    reset: () => setValues(erased(controller), { [options.param]: undefined }),
    render: ({ label, form }) => (
      <EnumControl
        mode="single"
        form={form}
        label={label}
        values={Object.values(options.values)}
        enumName={options.enumName}
        renderOption={options.renderOption as ((value: string) => ReactNode) | undefined}
        value={value}
        onChange={(selected) => setValues(erased(controller), { [options.param]: selected })}
      />
    ),
  };
}

/** Any number of values of an enum parameter that takes a list. */
export function enumSetFilter<Params extends DataSliceParams<string>, Key extends ParamName<Params, readonly string[]>>(
  controller: FilterController<Params>,
  options: EnumFilterOptions<Params, Key>,
): TableFilter {
  const value = valueOf<readonly string[]>(erased(controller), options.param);
  return {
    key: options.param,
    label: options.label,
    active: value !== undefined && value.length > 0,
    reset: () => setValues(erased(controller), { [options.param]: undefined }),
    render: ({ label, form }) => (
      <EnumControl
        mode="multiple"
        form={form}
        label={label}
        values={Object.values(options.values)}
        enumName={options.enumName}
        renderOption={options.renderOption as ((value: string) => ReactNode) | undefined}
        value={value}
        onChange={(selected) => setValues(erased(controller), { [options.param]: selected })}
      />
    ),
  };
}

type EnumControlProps = {
  form: FilterForm;
  label: string;
  values: readonly string[];
  enumName: string | undefined;
  renderOption: ((value: string) => ReactNode) | undefined;
} & (
  | { mode: "single"; value: string | undefined; onChange: (value: string | undefined) => void }
  | { mode: "multiple"; value: readonly string[] | undefined; onChange: (value: string[] | undefined) => void }
);

function EnumControl(props: EnumControlProps): ReactElement {
  const { t } = useTranslation();
  const options = props.values.map((enumValue) => ({
    value: enumValue,
    label: t(enumTranslationKeys(enumValue, props.enumName), { defaultValue: enumValue }),
    popoverLabel: props.renderOption?.(enumValue),
  }));
  return props.mode === "single" ? (
    <FilterByItems
      mode="single"
      form={props.form}
      placeholder={props.label}
      options={options}
      value={props.value}
      onChange={props.onChange}
    />
  ) : (
    <FilterByItems
      mode="multiple"
      form={props.form}
      placeholder={props.label}
      options={options}
      value={props.value}
      onChange={(selected) => props.onChange(selected === undefined ? undefined : [...selected])}
    />
  );
}

/** A yes/no question whose "no" is no filter, e.g. "only hanging modules". */
export function booleanFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  { param, label }: { param: ParamName<Params, boolean>; label: string },
): TableFilter {
  const value = valueOf<boolean>(erased(controller), param) === true;
  return {
    key: param,
    label,
    active: value,
    render: ({ label: translated, form }) => (
      <FilterByBooleanInput
        form={form}
        label={translated}
        value={value}
        onChange={(newValue) => setValues(erased(controller), { [param]: newValue ? true : undefined })}
      />
    ),
  };
}

/**
 * The archive state of an entity: a toggle group with the states only active, only archived and all, which `Filters`
 * shows below the secondary filters.
 */
export function archiveFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  {
    param = "archiveFilter" as ParamName<Params, ArchiveFilter>,
    label = "filterBar.archive",
  }: { param?: ParamName<Params, ArchiveFilter>; label?: string } = {},
): TableFilter {
  const value = valueOf<ArchiveFilter>(erased(controller), param) ?? ArchiveFilter.OnlyActive;
  return {
    key: param,
    label,
    active: value !== ArchiveFilter.OnlyActive,
    render: ({ label: translated }) => (
      <ArchiveScope label={translated} value={value} onChange={(newValue) => setValues(erased(controller), { [param]: newValue })} />
    ),
  };
}

/** The confirmation status of a draft-confirmed entity. */
export function confirmationStatusFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  {
    param = "confirmationStatus" as ParamName<Params, ConfirmationStatus>,
    label = "filterBar.confirmationStatus",
  }: { param?: ParamName<Params, ConfirmationStatus>; label?: string } = {},
): TableFilter {
  const value = valueOf<ConfirmationStatus>(erased(controller), param);
  return {
    key: param,
    label,
    active: value !== undefined,
    reset: () => setValues(erased(controller), { [param]: undefined }),
    render: ({ label: translated, form }) => (
      <FilterByConfirmationStatus
        form={form}
        label={translated}
        value={value}
        onChange={(newValue) => setValues(erased(controller), { [param]: newValue })}
      />
    ),
  };
}

/** A range needs at least one bound; a one-sided one names only `min` or only `max`. */
type BoundNames<Name extends string> = { min: Name; max: Name } | { min: Name; max?: undefined } | { min?: undefined; max: Name };

type NumberRangeOptions<Params extends DataSliceParams<string>> = BoundNames<ParamName<Params, number>> & { label: string; unit?: string };

function numberRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: NumberRangeOptions<Params>,
  numberKind: NumberRangeKind,
): TableFilter {
  const { min, max, label, unit } = options;
  const value: NumberRange = {
    min: min === undefined ? undefined : valueOf<number>(erased(controller), min),
    max: max === undefined ? undefined : valueOf<number>(erased(controller), max),
  };
  return {
    key: rangeKey(min, max),
    label,
    active: value.min !== undefined || value.max !== undefined,
    render: ({ label: translated, form }) => {
      const onChange = (range: NumberRange) => setValues(erased(controller), boundValues(min, range.min, max, range.max));
      return numberKind === "Year" ? (
        <FilterByYear form={form} label={translated} value={value} onChange={onChange} bounds={boundsOf(min, max)} />
      ) : (
        <FilterByNumberRange
          form={form}
          label={translated}
          value={value}
          onChange={onChange}
          numberKind={numberKind}
          bounds={boundsOf(min, max)}
          unit={unit}
        />
      );
    },
  };
}

/** An inclusive range of whole numbers, e.g. a count. */
export function integerRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: NumberRangeOptions<Params>,
): TableFilter {
  return numberRangeFilter(controller, options, "Integer");
}

/** An inclusive range of decimal numbers, e.g. a power in kWp. */
export function decimalRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: NumberRangeOptions<Params>,
): TableFilter {
  return numberRangeFilter(controller, options, "Decimal");
}

/** An inclusive range of amounts the request carries in cents and the user enters in euro. */
export function euroRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: BoundNames<ParamName<Params, number>> & { label: string },
): TableFilter {
  return numberRangeFilter(controller, options, "EuroCents");
}

/** An inclusive range of years. */
export function yearRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: BoundNames<ParamName<Params, number>> & { label: string },
): TableFilter {
  return numberRangeFilter(controller, options, "Year");
}

type DateRangeOptions<Params extends DataSliceParams<string>> = (
  | { from: ParamName<Params, string>; to: ParamName<Params, string> }
  | { from: ParamName<Params, string>; to?: undefined }
  | { from?: undefined; to: ParamName<Params, string> }
) & { label: string };

/**
 * An inclusive range of calendar days over a `LocalDate` column; the request carries `yyyy-MM-dd` in the user's
 * timezone.
 */
export function dateRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: DateRangeOptions<Params>,
): TableFilter {
  return dateRangeControl(controller, options, parseLocalDate, (date) => toLocalDate(date), FilterByDateRange);
}

/**
 * An inclusive range over an `Instant` column. The request carries ISO instants; a picked day sets `from` to its start
 * and `to` to its end.
 */
export function dateTimeRangeFilter<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  options: DateRangeOptions<Params>,
): TableFilter {
  return dateRangeControl(controller, options, parseInstant, (date) => date.toISOString(), FilterByDateTimeRange);
}

function dateRangeControl<Params extends DataSliceParams<string>>(
  controller: FilterController<Params>,
  { from, to, label }: DateRangeOptions<Params>,
  parse: (value: string) => Date | undefined,
  format: (date: Date) => string,
  Control: ComponentType<{
    form: FilterForm;
    label: string;
    value: OpenDateRange;
    onChange: (value: OpenDateRange) => void;
    bounds: RangeBounds;
  }>,
): TableFilter {
  const read = (param: string | undefined) => {
    const text = param === undefined ? undefined : valueOf<string>(erased(controller), param);
    return text === undefined ? undefined : parse(text);
  };
  const value: OpenDateRange = { from: read(from), to: read(to) };
  return {
    key: rangeKey(from, to),
    label,
    active: value.from !== undefined || value.to !== undefined,
    render: ({ label: translated, form }) => (
      <Control
        form={form}
        label={translated}
        value={value}
        onChange={(range) =>
          setValues(
            erased(controller),
            boundValues(
              from,
              range.from === undefined ? undefined : format(range.from),
              to,
              range.to === undefined ? undefined : format(range.to),
            ),
          )
        }
        bounds={boundsOf(from, to)}
      />
    ),
  };
}

function rangeKey(lower: string | undefined, upper: string | undefined): string {
  return `${lower ?? ""}-${upper ?? ""}`;
}

function boundsOf(lower: string | undefined, upper: string | undefined): RangeBounds {
  if (lower !== undefined && upper !== undefined) {
    return "Both";
  }
  return lower === undefined ? "Upper" : "Lower";
}

/** The values of the parameters of a range's ends; an end without a parameter stays out. */
function boundValues<Value>(
  lowerParam: string | undefined,
  lower: Value | undefined,
  upperParam: string | undefined,
  upper: Value | undefined,
): Record<string, Value | undefined> {
  return {
    ...(lowerParam === undefined ? {} : { [lowerParam]: lower }),
    ...(upperParam === undefined ? {} : { [upperParam]: upper }),
  };
}

/** What an entity filter hands to the select dialog of its entity. */
export interface EntityFilterSelectDialogProps<Preview> {
  /** The id the filter holds. */
  selectedId: string | undefined;
  /** The preview of that id, as the response's filter state echoes it; `undefined` while pending or unresolved. */
  selected: Preview | undefined;
  onSelect: (id: string | undefined) => void;
  onOpenChange: (open: boolean) => void;
}

interface EntityFilterOptions<Params extends DataSliceParams<string>, Preview, FilterState> {
  param: ParamName<Params, string>;
  label: string;
  /** The preview of the selected entity in the filter state the response echoes; `null` or `undefined` when unresolved. */
  preview: (filterState: FilterState | undefined) => Preview | null | undefined;
  renderPreview: (preview: Preview) => ReactNode;
  SelectDialog: ComponentType<EntityFilterSelectDialogProps<Preview>>;
}

/**
 * The id of an entity, picked in its select dialog. The filter state of the response says which entity the id names.
 */
export function entityFilter<Params extends DataSliceParams<string>, Preview, FilterState>(
  controller: FilterController<Params>,
  filterState: FilterState | undefined,
  options: EntityFilterOptions<Params, Preview, FilterState>,
): TableFilter {
  const { param, label, preview, renderPreview, SelectDialog } = options;
  const id = valueOf<string>(erased(controller), param);
  const resolved = preview(filterState);
  return {
    key: param,
    label,
    active: id !== undefined && id !== "",
    render: ({ label: translated, form }) => (
      <EntityControl
        form={form}
        label={translated}
        id={id}
        pending={filterState === undefined}
        resolved={resolved}
        renderPreview={renderPreview}
        onSelect={(newId) => setValues(erased(controller), { [param]: newId })}
        SelectDialog={SelectDialog}
      />
    ),
  };
}

interface EntityControlProps<Preview> {
  form: FilterForm;
  label: string;
  id: string | undefined;
  pending: boolean;
  resolved: Preview | null | undefined;
  renderPreview: (preview: Preview) => ReactNode;
  onSelect: (id: string | undefined) => void;
  SelectDialog: ComponentType<EntityFilterSelectDialogProps<Preview>>;
}

function EntityControl<Preview>({
  form,
  label,
  id,
  pending,
  resolved,
  renderPreview,
  onSelect,
  SelectDialog,
}: EntityControlProps<Preview>): ReactElement {
  const { t } = useEmbeddedTranslation();
  const selected = (() => {
    if (id === undefined) {
      return undefined;
    }
    if (pending) {
      return t("filterBar.previewPending");
    }
    return resolved === null || resolved === undefined ? t("filterBar.previewMissing") : renderPreview(resolved);
  })();
  return (
    <FilterByEntity
      form={form}
      label={label}
      active={id !== undefined}
      selected={selected}
      onClear={() => onSelect(undefined)}
      renderSelectDialog={(onOpenChange) => (
        <SelectDialog selectedId={id} selected={resolved ?? undefined} onSelect={onSelect} onOpenChange={onOpenChange} />
      )}
    />
  );
}
