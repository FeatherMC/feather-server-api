plugins {
    id("feather-server-api.java-conventions")
    id("feather-server-api.publishing-conventions")
}

dependencies {
    implementation(project(":api"))
}