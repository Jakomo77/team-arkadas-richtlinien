/*
 * GlassesControl - Android app to control Meta AI Glasses via the
 * Meta Wearables Device Access Toolkit (DAT SDK).
 */

import java.util.Properties

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

val localProperties = Properties().apply {
  val file = File(rootDir, "local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Meta Wearables Device Access Toolkit artifacts (mwdat-*)
    // Requires a GitHub personal access token with "read:packages" scope,
    // provided via the GITHUB_TOKEN env var or github_token in local.properties.
    maven {
      url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
      credentials {
        username = ""
        password = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("github_token") ?: ""
      }
    }
  }
}

rootProject.name = "GlassesControl"
include(":app")
