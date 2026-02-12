androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation("androidx.core:core-ktx:1.13.1")
        implementation(project(":utilities"))
    }
}
