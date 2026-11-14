// What Ladle does with any storyOrder that is not an array, its default included; `script-src 'self'` must turn it into an EvalError.
// oxlint-disable-next-line typescript/no-implied-eval
new Function("return 1")();
