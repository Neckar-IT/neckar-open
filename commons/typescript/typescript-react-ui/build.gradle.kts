import com.github.gradle.node.pnpm.task.PnpmTask

description = """ react component library """

// `storybook`, its `run` alias, `storybookBuild` and `integrationTest` come from the pnpm configuration —
// every package with the matching script gets them, with the worktree's Storybook port wired in.

tasks.register<PnpmTask>("generateComponentCatalog") {
  description = "Generates the committed component catalog (CATALOG.md) from the TypeScript source."
  group = "build"

  args.set(listOf("run", "catalog:generate"))

  inputs.dir("src")
  inputs.dir("scripts")
  inputs.file("package.json")
  outputs.file(layout.projectDirectory.file("CATALOG.md"))
}

