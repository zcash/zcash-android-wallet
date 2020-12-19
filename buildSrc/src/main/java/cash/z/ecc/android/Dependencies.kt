package cash.z.ecc.android

object Deps {
    // For use in the top-level build.gradle which gives an error when provided
    // `Deps.Kotlin.version` directly
    const val kotlinVersion =       "1.4.10"

    const val compileSdkVersion =   29
    const val buildToolsVersion =   "29.0.2"
    const val minSdkVersion =       21
    const val targetSdkVersion =    29
    const val versionName =         "1.0.0-alpha37"
    const val versionCode =         1_00_00_037  // last digits are alpha(0XX) beta(2XX) rc(4XX) release(8XX). Ex: 1_08_04_401 is an release candidate build of version 1.8.4 and 1_08_04_800 would be the final release.
    const val packageName =         "cash.z.ecc.android"


    object AndroidX {
        const val ANNOTATION =              "androidx.annotation:annotation:1.1.0"
        const val APPCOMPAT =               "androidx.appcompat:appcompat:1.1.0"
        const val BIOMETRICS =              "androidx.biometric:biometric:1.2.0-alpha01"
        const val CONSTRAINT_LAYOUT =       "androidx.constraintlayout:constraintlayout:1.1.3"
        const val CORE_KTX =                "androidx.core:core-ktx:1.1.0"
        const val FRAGMENT_KTX =            "androidx.fragment:fragment-ktx:1.1.0-beta01"
        const val LEGACY =                  "androidx.legacy:legacy-support-v4:1.0.0"
        const val MULTIDEX =                "androidx.multidex:multidex:2.0.1"
        const val PAGING =                  "androidx.paging:paging-runtime-ktx:2.1.2"
        const val RECYCLER =                "androidx.recyclerview:recyclerview:1.2.0-alpha05"
        object CameraX :         Version("1.0.0-beta04") {
            val CAMERA2 =                   "androidx.camera:camera-camera2:1.0.0-beta04"
            val CORE =                      "androidx.camera:camera-core:1.0.0-beta04"
            val LIFECYCLE =                 "androidx.camera:camera-lifecycle:1.0.0-beta04"
            object View :       Version("1.0.0-alpha11") {
                val EXT =                   "androidx.camera:camera-extensions:1.0.0-alpha11"
                val VIEW =                  "androidx.camera:camera-view:1.0.0-alpha11"
            }
        }
        object Lifecycle :       Version("2.2.0") {
            val LIFECYCLE_RUNTIME_KTX =     "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            val LIFECYCLE_EXTENSIONS =      "androidx.lifecycle:lifecycle-extensions:$version"
        }
        object Navigation :     Version("2.3.0") {
            val FRAGMENT_KTX =              "androidx.navigation:navigation-fragment-ktx:$version"
            val UI_KTX =                    "androidx.navigation:navigation-ui-ktx:$version"
        }
        object Room :           Version("2.2.5") {
            val ROOM_COMPILER =             "androidx.room:room-compiler:$version"
            val ROOM_KTX =                  "androidx.room:room-ktx:$version"
        }
    }
    object Dagger :             Version("2.25.2") {
        val ANDROID_SUPPORT =               "com.google.dagger:dagger-android-support:$version"
        val ANDROID_PROCESSOR =             "com.google.dagger:dagger-android-processor:$version"
        val COMPILER =                      "com.google.dagger:dagger-compiler:$version"
    }
    object Google {
        // solves error: Duplicate class com.google.common.util.concurrent.ListenableFuture found in modules jetified-guava-26.0-android.jar (com.google.guava:guava:26.0-android) and listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0)
        // per this recommendation from Chris Povirk, given guava's decision to split ListenableFuture away from Guava: https://groups.google.com/d/msg/guava-discuss/GghaKwusjcY/bCIAKfzOEwAJ
        const val GUAVA =                   "com.google.guava:guava:27.0.1-android"
        const val MATERIAL =                "com.google.android.material:material:1.1.0-beta01"
        // QR Scanner
        const val ML_VISION =               "com.google.firebase:firebase-ml-vision:24.0.3"
    }
    object Grpc :               Version("1.25.0") {
        val ANDROID =                       "io.grpc:grpc-android:$version"
        val OKHTTP =                        "io.grpc:grpc-okhttp:$version"
        val PROTOBUG =                      "io.grpc:grpc-protobuf-lite:$version"
        val STUB =                          "io.grpc:grpc-stub:$version"
    }
    object Analytics { // for dogfooding/crash-reporting/feedback only on internal team builds
        val BUGSNAG =                       "com.bugsnag:bugsnag-android:5.0.1"
        val MIXPANEL =                      "com.mixpanel.android:mixpanel-android:5.6.3"
    }
    object JavaX {
        const val INJECT =                   "javax.inject:javax.inject:1"
        const val JAVA_ANNOTATION =          "javax.annotation:javax.annotation-api:1.3.2"
    }
    object Kotlin :             Version(kotlinVersion) {
        val STDLIB =                         "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        object Coroutines :     Version("1.3.9") {
            val ANDROID =                    "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            val CORE =                       "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            val TEST =                       "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }
    object Zcash {
        const val ANDROID_WALLET_PLUGINS =   "cash.z.ecc.android:zcash-android-wallet-plugins:1.0.0"
        const val KOTLIN_BIP39 =            "cash.z.ecc.android:kotlin-bip39:1.0.1"
        object Sdk :            Version("1.2.1-beta03") {
            val MAINNET =                    "cash.z.ecc.android:zcash-android-sdk-mainnet:$version"
            val TESTNET =                    "cash.z.ecc.android:zcash-android-sdk-testnet:$version"
        }
    }
    object Misc {
        const val LOTTIE =                   "com.airbnb.android:lottie:3.1.0"
        const val CHIPS =                    "com.github.gmale:chips-input-layout:2.3.1"
        object Plugins {
            const val SECURE_STORAGE =       "com.github.gmale:secure-storage-android:0.0.3"//"de.adorsys.android:securestoragelibrary:1.2.2"
            const val QR_SCANNER =           "com.google.zxing:core:3.2.1"
        }
    }

    object Test {
        const val JUNIT =                    "junit:junit:4.12"
        const val MOKITO =                   "org.mockito:mockito-android:3.5.10"
        const val MOKITO_KOTLIN =            "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
        const val COROUTINES_TEST =          "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9"
        object Android {
            const val JUNIT =                "androidx.test.ext:junit:1.1.1"
            const val ESPRESSO =             "androidx.test.espresso:espresso-core:3.2.0"
        }
    }
}

open class Version(@JvmField val version: String)

