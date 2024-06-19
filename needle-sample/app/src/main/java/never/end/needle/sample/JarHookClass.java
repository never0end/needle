package never.end.needle.sample;


import android.util.Log;

import never.end.gradle.lib.annotations.TargetClass;
import never.end.needle.sample.lib.LibTargetClass;

@TargetClass({"never.end.needle.sample.lib.LibTargetClass"})
public class JarHookClass extends LibTargetClass implements MyInterface{

    private static final String TAG;
    private static final MyInterface staticInterface;

    static {
        TAG = "JarHookClass";
        staticInterface = new MyInterface() {
            @Override
            public void printInterface() {
                Log.e(TAG, "JarHookClass static interface class");
            }
        };
        staticInterface.printInterface();
    }

    public JarHookClass(String s) {

    }

    @Override
    public void printInterface() {
        Log.i(TAG, "JarHookClass print Interface");
    }

}