package never.end.needle.sample;

import android.util.Log;

public class ParentClass extends AncestorClass{
    private static final String TAG = "ParentClass";

    private String a;
    private OtherChild proxy;

    ParentClass() {
//        proxy = new ProxyClass();
    }

    ParentClass(String s) {
        Log.e(TAG, "Parent class with string： " + s);
//        proxy = new ProxyClass(s);
    }

    public void printMySelf() {
        if(proxy != null) {
            proxy.printMySelf();
            return;
        }

        Log.e(TAG, "This class is: " + this.getClass().getCanonicalName());
    }
    
    public String getFatherClassName() {
        return "this is Parent class";
    }
}
