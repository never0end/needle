package never.end.gradle.lib.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@java.lang.annotation.Target(ElementType.TYPE)
public @interface TargetClass {
    String[] value();
}
