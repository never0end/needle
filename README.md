# Needle
[中文说明](README-CN.md)

Needle is an Android java class replacement tool implemented using ASM Tools.

## Class Replacement
In daily development, there are multiple common modules or JAR packages in the project. Many of them are the products of the company's internal infrastructure or other third-party SDKs. During the customized development process, these common libraries cannot be modified flexibly.
And the traditional Hook scheme has the following problems:
1. It is difficult to find the appropriate Hook position, which relies heavily on the internal logic of the object. The possibility of point failure is high after the version upgrade.
2. There are many invasive codes during runtime, increasing the runtime consumption.
3. It is difficult to implement complex scenarios such as method, parameter or the entire class logic replacement.
4. The invasive code has no OO syntax constraints and is prone to runtime errors.

Based on the above problems, if `class replacement` during compilation can be achieved, the ability to replace common logic simply and quickly can be realized.
## Start

### Reference Configuration

#### Root build.gradle
```groovy
buildscript {
    repositories {
        ...
    }

    dependencies {
        // classpath
        ...
        classpath 'io.github.never0end:needle-plugin:0.1'
    }
}
```

#### model build.gradle
```groovy
apply plugin: 'never.end.needle-plugin'
android {
    ...
}

dependencies {
    implementation 'io.github.never0end:needle-lib:0.1'
    annotationProcessor 'io.github.never0end:needle-compiler:0.1'
}
```

### Coding
Needle uses annotations to specify the class to be replaced, and the injected class must be a subclass of the target class.

This is sample code:
```kotlin
@TargetClass("never.end.needle.sample.FirstActivity\$StaticInnerClass")
class HookInnerClass : FirstActivity.StaticInnerClass() {
    fun printMyself() {
        Log.d("HookInnerClass", "this is HookInnerClass")
    }
}
```

### Comments
1. Replace location：All the polymorphic replacements of objects in the code are completed during the compilation period.
2. Replace Rules：It must be the subclass replacing the parent class. In this way, there will be no side effects after compilation.
3. Replace Scope：Supports for java/jar/aar files in the project.