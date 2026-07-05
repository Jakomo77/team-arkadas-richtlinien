import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.jetbrains.kotlin.serialization)
  alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
  val file = rootProject.file("local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String): String = localProperties.getProperty(key) ?: ""

android {
  namespace = "com.jakob.glassescontrol"
  compileSdk = 36

  buildFeatures {
    compose = true
    buildConfig = true
  }

  defaultConfig {
    applicationId = "com.jakob.glassescontrol"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    // Meta Wearables Device Access Toolkit setup.
    // Left blank while "Entwicklungsmodus" is enabled in the Meta AI app -
    // the SDK then auto-registers this app against your paired glasses.
    manifestPlaceholders["mwdatApplicationId"] = localProp("mwdat_application_id")
    manifestPlaceholders["mwdatClientToken"] = localProp("mwdat_client_token")

    buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProp("anthropic_api_key")}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.exifinterface)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  // Meta Wearables Device Access Toolkit
  implementation(libs.mwdat.core)
  implementation(libs.mwdat.camera)
  implementation(libs.mwdat.mockdevice)
}
