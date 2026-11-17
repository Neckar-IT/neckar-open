import { type ProcessResult } from "./processResult";

/**
 * An operation that can be applied to a sequence.
 */
export interface Operation<From, To> {
  process(value: From, index: number): ProcessResult<To>;
}

/**
 * Operation that is analogous to Array.prototype.filter().
 */
export class FilterOperation<Value> implements Operation<Value, Value> {
  private readonly predicate: (value: Value, index: number) => boolean;
  constructor(predicate: (value: Value, index: number) => boolean) {
    this.predicate = predicate;
  }

  process(value: Value, index: number): ProcessResult<Value> {
    return this.predicate(value, index) ? { value, resultType: "take" } : { resultType: "skip" };
  }
}

/**
 * Operation that is analogous to Array.prototype.map().
 */
export class MapOperation<From, To> implements Operation<From, To> {
  private readonly mapper: (value: From, index: number) => To;
  constructor(mapper: (value: From, index: number) => To) {
    this.mapper = mapper;
  }

  process(value: From, index: number): ProcessResult<To> {
    return { value: this.mapper(value, index), resultType: "take" };
  }
}

/**
 * Casts a value.
 */
export class CastOperation<From, To> implements Operation<From, To> {
  process(value: From): ProcessResult<To> {
    return { value: value as unknown as To, resultType: "take" };
  }
}
