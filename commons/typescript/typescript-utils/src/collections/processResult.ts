/**
 * Indicates an intermediate operation that provides a computed value;
 */
export type TakeResult<Value> = { value: Value; resultType: "take" };

/**
 * Indicates the result of the operation should be ignored.
 */
export type SkipResult = { resultType: "skip" };

/**
 * The result of a terminal operation.
 */
export type StopResult = { resultType: "stop" };

/**
 * Result of a process operation.
 */
export type ProcessResult<Value> = TakeResult<Value> | SkipResult | StopResult;
