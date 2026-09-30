import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val buildGgwaveIos = tasks.register<Exec>("buildGgwaveIos") {
        workingDir = project.file("src/iosMain/cpp")
        commandLine("bash", "build-ggwave-ios.sh")

        inputs.files(project.fileTree("src/nativeShared/ggwave"))

        outputs.file("src/iosMain/cpp/iphoneos/libggwave.a")
        outputs.file("src/iosMain/cpp/iphonesimulator/libggwave.a")

        onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        val sdkSubdir = when (iosTarget.name) {
            "iosArm64" -> "iphoneos"
            "iosSimulatorArm64" -> "iphonesimulator"
            else -> error("Target iOS no esperado: ${iosTarget.name}")
        }
        iosTarget.compilations.getByName("main").cinterops.create("ggwave") {
            defFile(project.file("src/nativeInterop/cinterop/ggwave.def"))
            includeDirs(project.file("src/iosMain/cpp/include"))
            extraOpts("-libraryPath", project.file("src/iosMain/cpp/$sdkSubdir").absolutePath)
        }

        tasks.named("cinteropGgwave${iosTarget.name.replaceFirstChar { it.uppercase() }}").configure {
            dependsOn(buildGgwaveIos)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.cryptography.core)
            implementation(libs.curve25519.kotlin)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.uuid)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)

            implementation(libs.koin.android)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.cryptography.provider.jdk)

            implementation(libs.androidx.biometric)
            implementation(libs.androidx.security.crypto)

            implementation("androidx.exifinterface:exifinterface:1.3.7")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
            implementation(libs.cryptography.provider.apple)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.cryptography.provider.jdk)
        }
    }
}

android {
    namespace = "uoc.edu.psirvent.murmr"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uoc.edu.psirvent.murmr"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-fexceptions", "-O2")
                arguments("-DANDROID_STL=c++_static")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

sqldelight {
    databases {
        create("MurmrDatabase") {
            packageName.set("uoc.edu.psirvent.murmr.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))

            verifyMigrations.set(false)
        }
    }
}

compose.desktop {
    application {
        mainClass = "uoc.edu.psirvent.murmr.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Murmr"
            packageVersion = "1.0.0"
            description = "Mensajería E2EE con emparejamiento acústico ultrasónico"
            copyright = "© 2026 Pablo Sirvent - UOC TFG"
            vendor = "UOC TFG"

            modules("java.sql", "java.naming", "jdk.unsupported", "jdk.crypto.ec")

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "Murmr"
                upgradeUuid = "8a4b2c10-7e91-4a1c-9b4e-2d3f8e1c5a02"
                shortcut = true
                menu = true
                dirChooser = true
            }
            macOS {
                iconFile.set(project.file("icons/icon.png"))
                bundleID = "uoc.edu.psirvent.murmr"
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
                packageName = "murmr"
            }
        }
    }
}
