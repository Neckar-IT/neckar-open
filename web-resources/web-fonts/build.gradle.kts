import de.undercouch.gradle.tasks.download.Download

/**
 * Downloads all fonts by calling
 *
 * gradle downloadFonts
 *
 * to src/main/resources
 */

plugins {
  download
}

/**
 * fonts.google.com serves a JavaScript file in place of the font files, so the variable .ttf files come from github.com/google/fonts.
 */
val downloadGoogleFonts: TaskProvider<Download> = tasks.register<Download>("downloadGoogleFonts") {
  description = "Downloads Google fonts"
  group = "resources"

  val url2FileNames: Map<String, String> = mapOf(
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/opensans/OpenSans%5Bwdth,wght%5D.ttf" to "OpenSans.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/opensans/OpenSans-Italic%5Bwdth,wght%5D.ttf" to "OpenSans-Italic.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/roboto/Roboto%5Bwdth,wght%5D.ttf" to "Roboto.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/robotocondensed/RobotoCondensed%5Bwght%5D.ttf" to "RobotoCondensed.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/robotoflex/RobotoFlex%5BGRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght%5D.ttf" to "RobotoFlex.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/nunito/Nunito%5Bwght%5D.ttf" to "Nunito.ttf",
    "https://github.com/google/fonts/raw/refs/heads/main/ofl/oswald/Oswald%5Bwght%5D.ttf" to "Oswald.ttf",

    "https://github.com/google/material-design-icons/raw/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf" to "MaterialIcons.ttf",
  )

  src(url2FileNames.keys)
  eachFile {
    name = url2FileNames[this.sourceURL.toString()]
  }

  dest("src/main/resources/variable-webfonts")
  overwrite(true)
  tempAndMove(true)
}


val downloadZipFiles: Download by tasks.register<Download>("downloadZipFiles") {
  description = "Downloads zipped fonts"
  group = "resources"

  //Update the download link from here:
  //
  // https://fontawesome.com/download
  //

  src("https://use.fontawesome.com/releases/v6.3.0/fontawesome-free-6.3.0-web.zip")
  dest("build/download/zipped/fontawesome-free.zip")

  overwrite(true)
  tempAndMove(true)
}

val unzipDownloadedFiles: TaskProvider<Copy> = tasks.register<Copy>("unzipDownloadedFiles") {
  dependsOn(downloadZipFiles)

  from(zipTree(downloadZipFiles.dest)) {
    include("**/*.ttf")
    include("**/fontawesome.min.css")
    include("**/regular.min.css")
    include("**/solid.min.css")
    include("**/brands.min.css")
  }

  into("src/main/resources")
  includeEmptyDirs = false

  eachFile {
    //Remove the leading directory
    path = this.path.substringAfter("/")
  }
}


tasks.register("downloadFonts") {
  dependsOn(downloadGoogleFonts, unzipDownloadedFiles)
}

