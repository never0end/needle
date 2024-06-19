package never.end.needle.sample;


import android.util.Log;

import never.end.gradle.lib.annotations.TargetClass;

@TargetClass({"never.end.needle.sample.ParentClass"})
public class HookClass extends ParentClass implements MyInterface{

    private static final String TAG;
    private static final MyInterface staticInterface;

    static {
        TAG = "HookClass";
        staticInterface = new MyInterface() {
            @Override
            public void printInterface() {
                Log.e(TAG, "HookClass static interface class");
            }
        };
        staticInterface.printInterface();
    }

    public HookClass() {
        ParentClass parentClass = new ParentClass();
    }

    public HookClass(String s) {

    }

    @Override
    public void printMySelf() {
        Log.i(TAG, "HookClass print self");
    }

    @Override
    public void printInterface() {
        Log.i(TAG, "HookClass print Interface");
    }

    @Override
    public String getFatherClassName() {
        return "this is Hook Class";
    }

    public void innerClassCreate() {
        MyInterface myInterface = new MyInterface() {
            @Override
            public void printInterface() {
                Log.e(TAG, "this is inner no name MyInterface class");
            }
        };

        KotlinInterface kotlinInterface = new KotlinInterface() {
            @Override
            public void kotlinInterfacePrint() {
                Log.e(TAG, "HookClass kotlin inner class print");
            }
        };
    }

    public static void staticFunctionCall() {
        Log.e(TAG, "HookClass static function call");
    }
}