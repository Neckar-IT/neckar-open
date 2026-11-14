import { CastOperation, FilterOperation, MapOperation, type Operation } from "./operation";
import { type ProcessResult } from "./processResult";
import { type Sequence } from "./sequence";

export class Step<Source, From, To> implements Sequence<To>, Operation<From, To> {
  /**
   * The original elements the sequence should be applied to.
   */
  private readonly source: Source[];
  /**
   * The operation of the previous step.
   */
  private prevOperation: Operation<unknown, From>;
  /**
   * The operation of this step.
   */
  private operation: Operation<From, To>;

  constructor(source: Source[], prevOperation?: Operation<unknown, From>, operation?: Operation<From, To>) {
    this.source = source;
    // We could make the previous and/or the current operation optional but this would result
    // in less readable code. The cast-operations do not change something semantically and serve
    // as a fallback here.
    this.prevOperation = prevOperation ?? new CastOperation<unknown, From>();
    this.operation = operation ?? new CastOperation<From, To>();
  }

  toArray(): To[] {
    const result: To[] = [];
    for (let i = 0; i < this.source.length; i++) {
      const prevResult = this.prevOperation.process(this.source[i], i);
      switch (prevResult.resultType) {
        case "skip":
          continue;
        case "take":
          break;
        case "stop":
          return result;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
      const currentResult = this.operation.process(prevResult.value, i);
      switch (currentResult.resultType) {
        case "skip":
          continue;
        case "take":
          result.push(currentResult.value);
          break;
        case "stop":
          return result;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
    }
    return result;
  }

  find(predicate: (value: To, index: number) => boolean): To | undefined {
    for (let i = 0; i < this.source.length; i++) {
      const prevResult = this.prevOperation.process(this.source[i], i);
      switch (prevResult.resultType) {
        case "skip":
          continue;
        case "take":
          break;
        case "stop":
          return undefined;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
      const currentResult = this.operation.process(prevResult.value, i);
      switch (currentResult.resultType) {
        case "skip":
          continue;
        case "take":
          if (predicate(currentResult.value, i)) {
            return currentResult.value;
          }
          break;
        case "stop":
          return undefined;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
    }
    return undefined;
  }

  findIndex(predicate: (value: To, index: number) => boolean): number {
    for (let i = 0; i < this.source.length; i++) {
      const prevResult = this.prevOperation.process(this.source[i], i);
      switch (prevResult.resultType) {
        case "skip":
          continue;
        case "take":
          break;
        case "stop":
          return -1;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
      const currentResult = this.operation.process(prevResult.value, i);
      switch (currentResult.resultType) {
        case "skip":
          continue;
        case "take":
          if (predicate(currentResult.value, i)) {
            return i;
          }
          break;
        case "stop":
          return -1;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
    }
    return -1;
  }

  every(predicate: (value: To, index: number) => boolean): boolean {
    for (let i = 0; i < this.source.length; i++) {
      const prevResult = this.prevOperation.process(this.source[i], i);
      switch (prevResult.resultType) {
        case "skip":
          continue;
        case "take":
          break;
        case "stop":
          return true;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
      const currentResult = this.operation.process(prevResult.value, i);
      switch (currentResult.resultType) {
        case "skip":
          continue;
        case "take":
          if (!predicate(currentResult.value, i)) {
            return false;
          }
          break;
        case "stop":
          return true;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
    }
    return true;
  }

  some(predicate: (value: To, index: number) => boolean): boolean {
    for (let i = 0; i < this.source.length; i++) {
      const prevResult = this.prevOperation.process(this.source[i], i);
      switch (prevResult.resultType) {
        case "skip":
          continue;
        case "take":
          break;
        case "stop":
          return false;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
      const currentResult = this.operation.process(prevResult.value, i);
      switch (currentResult.resultType) {
        case "skip":
          continue;
        case "take":
          if (predicate(currentResult.value, i)) {
            return true;
          }
          break;
        case "stop":
          return false;
        default:
          throw new Error(`Unexpected value in Switch Statement: resultType`);
      }
    }
    return false;
  }

  process(value: From, index: number): ProcessResult<To> {
    // Apply the whole ancestor chain, not just this step's own operation: run prevOperation first,
    // then operation. Without this, a chain of 3+ operations would silently drop everything except
    // the last two operations.
    const prevResult = this.prevOperation.process(value, index);
    if (prevResult.resultType !== "take") {
      return prevResult;
    }
    return this.operation.process(prevResult.value, index);
  }

  filter(predicate: (value: To, index: number) => boolean): Sequence<To> {
    return new Step(this.source, this, new FilterOperation(predicate));
  }

  map<V>(mapper: (value: To, index: number) => V): Sequence<V> {
    return new Step(this.source, this, new MapOperation(mapper));
  }
}
