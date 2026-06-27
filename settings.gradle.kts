pluginManagement {
    repositories {
        // 阿里云主源
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 腾讯云兜底
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-google/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugin/") }

        // Maven Central 官方兜底
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云主源
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 腾讯云兜底
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-google/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // Maven Central 官方兜底
        mavenCentral()
        google()
    }
}

rootProject.name = "BuKeBiao"
include(":app")
