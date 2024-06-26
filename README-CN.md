# Needle
Needle 是基于字节码修改技术实现的Android java 类替换工具。

## 类替换
在日常开发中，工程中存在多个公共模块或者JAR包，很多是公司内部基建产物或者其他第三方SDK。在定制化开发过程中，无法灵活修改这些公共库。
且传统插桩方案有如下问题：
1. 寻找合适插桩位置困难，严重依赖对象的内部逻辑，版本升级后点位失效可能性高
2. 运行时侵入代码较多，增加运行时耗时
3. 方法、参数或整个类逻辑替换等复杂场景实现困难
4. 侵入代码无OO语法约束，容易产生运行时错误

基于以上问题，如果能够实现编译期间`类替换`，即可实现简单，快速的公共逻辑替换能力。

## 开始使用

### 引用配置

#### 添加工程根目录脚本插件引用
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

#### 项目模块脚本引用
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

### 使用
Needle使用注解指定要替换的类，且注入类必须是被替换类的子类。
```kotlin
@TargetClass("never.end.needle.sample.FirstActivity\$StaticInnerClass")
class HookInnerClass : FirstActivity.StaticInnerClass() {
    fun printMyself() {
        Log.d("HookInnerClass", "this is HookInnerClass")
    }
}
```

### 适用场景
1. 替换方式：在编译期完成代码中所有对象的多态替换。
2. 替换要求：必须是子类替换父类，编译后没有任何副作用
3. 替换范围：支持源代码替换和未混淆的jar、aar包替换