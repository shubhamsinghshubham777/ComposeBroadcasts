plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish.plugin)
}

android {
    namespace = "compose.broadcasts"
    compileSdk = 37

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(groupId = "io.github.shubhamsinghshubham777", artifactId = "composebroadcasts")
    pom {
        name.set("Compose Broadcasts")
        description.set("Compose Broadcasts provides the Jetpack Compose way of writing Broadcast Receiver code.")
        inceptionYear.set("2024")
        url.set("https://github.com/shubhamsinghshubham777/ComposeBroadcasts")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("shubhamsinghshubham777")
                name.set("Shubham Singh")
                url.set("https://github.com/shubhamsinghshubham777/")
            }
        }
        scm {
            url.set("https://github.com/shubhamsinghshubham777/ComposeBroadcasts")
            connection.set("scm:git:git://github.com/shubhamsinghshubham777/ComposeBroadcasts.git")
            developerConnection.set("scm:git:ssh://git@github.com/shubhamsinghshubham777/ComposeBroadcasts.git")
        }
    }
}
