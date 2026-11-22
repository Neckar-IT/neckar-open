import it.neckar.projects.sharing.OpenSharedFiles
import it.neckar.projects.sharing.publishFiles

description = """Cent arithmetic with the semantics of Money for pricing scripts"""

publishFiles(OpenSharedFiles.ScriptMoneySource, file("src/scriptMoney.ts"))
publishFiles(OpenSharedFiles.ScriptMoneyCases, file("src/scriptMoney.cases.json"))
