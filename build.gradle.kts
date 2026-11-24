/*
 * Root plugin declarations shared by modules. Runtime dependencies and Android packaging options live in app/build.gradle.kts.
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
}
