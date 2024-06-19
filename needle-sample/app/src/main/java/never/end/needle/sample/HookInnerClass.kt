package never.end.needle.sample

import android.util.Log
import never.end.gradle.lib.annotations.TargetClass

@TargetClass("never.end.needle.sample.FirstActivity\$StaticInnerClass")
class HookInnerClass : FirstActivity.StaticInnerClass() {
    fun printMyself() {
        Log.d("HookInnerClass", "this is HookInnerClass")
    }
}