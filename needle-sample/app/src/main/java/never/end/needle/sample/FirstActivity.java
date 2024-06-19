package never.end.needle.sample;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import never.end.needle.sample.lib.LibTargetClass;

public class FirstActivity extends AppCompatActivity {
    private static final ParentClass staticClass = new ParentClass();
    private static final LibTargetClass jarClass = new LibTargetClass();

    private innerClassFiledTest innerClassFiledTest = new innerClassFiledTest();

    private StaticInnerClass staticInnerClass = new StaticInnerClass();
    private static final StringBuilder sb = new StringBuilder();

    static {
        sb.append(", \n the staticClass is ").append(staticClass.getClass().getCanonicalName());
        sb.append(", \n the jarClass is ").append(jarClass.getClass().getCanonicalName());
        staticFunctionCall();
    }

    private static void staticFunctionCall() {
        ParentClass staticFunctionClass = new ParentClass();
        sb.append("\n the staticFunctionClass is ").append(staticFunctionClass.getClass().getCanonicalName());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ParentClass myClass = new ParentClass();
        myClass.printMySelf();

        OtherChild otherChild = new OtherChild("proxy class");
        otherChild.printMySelf();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_first);

        TextView textView = findViewById(R.id.className);

        InnerOtherChild innerOtherChild =  new InnerOtherChild();

        sb.append(", \n The myClass is ")
                .append(myClass.getClass().getCanonicalName())
                .append(", \n the otherChild father is ")
                .append(otherChild.getFatherClassName());

        sb.append(", \n innerOtherChild father is ")
                .append(innerOtherChild.getFatherClassName());

        sb.append(", \n the staticInnerClass self is ").append(staticInnerClass.getClass().getCanonicalName());

        sb.append(", \n the staticInnerClass father is ").append(staticInnerClass.getFatherClassName());


        textView.setText(sb.toString());
    }

    public class innerClassFiledTest {
        private ParentClass classInit;
        private ParentClass innerClassFiled = new ParentClass();

        public innerClassFiledTest() {
            classInit = new ParentClass();
            sb.append(",\n classInit is ").append(classInit.getClass().getCanonicalName());
            sb.append(",\n innerClassFiled is ").append(innerClassFiled.getClass().getCanonicalName());
            innerClassFunction();
        }

        public void innerClassFunction() {
            ParentClass innerFunction = new ParentClass();
            sb.append(",\n innerFunction is ").append(innerFunction.getClass().getCanonicalName());
        }

    }

    public static class StaticInnerClass extends ParentClass{
        ParentClass staticInnerFiled = new ParentClass();
        static ParentClass staticInner = new ParentClass();

        static {
            sb.append(",\n staticInner is ").append(staticInner.getClass().getCanonicalName());
        }
        public StaticInnerClass() {
            ParentClass staticInnerInit = new ParentClass();
            sb.append(",\n staticInnerInit is ").append(staticInnerInit.getClass().getCanonicalName());
            sb.append(",\n staticInnerFiled is ").append(staticInnerFiled.getClass().getCanonicalName());

            staticInnerFunction();
            staticInnerStaticFunction();
        }

        public void staticInnerFunction() {
            ParentClass staticInnerFunction = new ParentClass();
            sb.append(",\n staticInnerFunction is ").append(staticInnerFunction.getClass().getCanonicalName());
        }

        public static void staticInnerStaticFunction() {
            ParentClass staticInnerStaticFunction = new ParentClass();
            sb.append(",\n staticInnerStaticFunction is ").append(staticInnerStaticFunction.getClass().getCanonicalName());
        }
    }

    public class InnerOtherChild extends ParentClass {
        private ParentClass innerOtherChildInit;
        public InnerOtherChild() {
            innerOtherChildInit = new ParentClass();

            sb.append(", \n innerOtherChildInit is ").append(innerOtherChildInit.getClass());
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }
}
