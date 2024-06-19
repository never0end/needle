package never.end.gradle.processer;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import never.end.gradle.lib.annotations.TargetClass;
import never.end.gradle.lib.needle.NeedleConst;


@AutoService(Processor.class)
public class NeedleAnnoProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elements;
    private Filer filer;
    private Types types;

    private HashMap<String, List<String>> results = new HashMap<>();

    private static final Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

    static {
        annotations.add(TargetClass.class);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "processing...");
        boolean newElements = false;
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(TargetClass.class);
        for (Element e : annotatedElements) {
            if (e instanceof TypeElement) {
                String sourceName = ((TypeElement) e).getQualifiedName().toString();
                if (results.get(sourceName) != null) {
                    continue;
                }

                String[] targetNames = ((TypeElement) e).getAnnotation(TargetClass.class).value();

                for (String target : targetNames) {
                    Element targetE = elements.getTypeElement(target);

                    if (targetE == null){
                        messager.printMessage(Diagnostic.Kind.ERROR, "target class <"+target+"> dose not exist!");
                        throw new IllegalArgumentException("Needle target dose not exist!!");
                    }

                    if (!checkSuperOrInterface(target, (TypeElement)e)) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "target class：<"+target+"> must be the superClass of <"+sourceName+"> class!");
                        throw new IllegalArgumentException("Needle target is not the superClass!!");
                    }

                    for(List<String> value: results.values()) {
                        if (value.contains(target)) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "needle found duplicate target annotation ! target class: " + target + " , source file: "+sourceName );
                            throw new IllegalArgumentException("Needle duplicate target!!");
                        }
                    }

                }
                results.put(sourceName, Arrays.asList(targetNames));
                messager.printMessage(Diagnostic.Kind.NOTE, "needle found class: " + sourceName + "with replace list: "+ Arrays.toString(targetNames));
                newElements = true;
            }
        }

        if(results.size() > 0 && newElements) {
            try {
//                JavaFileObject javaFileObject = filer.createClassFile("never.end.gradle.NeedleAnnoMappingSource");
//                messager.printMessage(Diagnostic.Kind.NOTE, "Needle create java mapping file success：" + javaFileObject.toUri());
//
//                JavaFileObject sourceFileObject = filer.createSourceFile("never.end.gradle.NeedleAnnoMappingSource");
//                messager.printMessage(Diagnostic.Kind.NOTE, "Needle create source mapping file success：" + sourceFileObject.toUri());

                FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, NeedleConst.PACKAGE, "needle.mapping");
                OutputStream outputStream = fileObject.openOutputStream();
                StringBuilder builder = new StringBuilder();
                for (String key: results.keySet()) {
                    builder.append(key).append(":").append(mapClassString(results.get(key))).append("\r\n");
                }

                outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.close();

//                outputStream = javaFileObject.openOutputStream();
//                outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
//                outputStream.close();

                messager.printMessage(Diagnostic.Kind.NOTE, "Needle create mapping file success：" + fileObject.toUri());

            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Needle create mapping file failed！ ");

                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private String mapClassString(List<String> names) {
        if (names == null || names.size() == 0)
            return "";

        StringBuilder builder = new StringBuilder();
        builder.append(names.get(0));
        for (int i = 1; i < names.size(); i++) {
            builder.append(",").append(names.get(i));
        }

        return builder.toString();
    }

    private boolean checkSuperOrInterface(String target, TypeElement e) {
        String sourceSuperName = ((DeclaredType) e.getSuperclass()).asElement().getSimpleName().toString();
        List<? extends TypeMirror> interfaceName = ((TypeElement) e).getInterfaces();
        messager.printMessage(Diagnostic.Kind.NOTE, "checkSuperOrInterface target: "+target
            + ", Source superClass: "+sourceSuperName);

        if (types.isSameType(((TypeElement) e).getSuperclass(), elements.getTypeElement(target).asType())){
            return true;
        }

        for (TypeMirror t:interfaceName) {
            String name = ((DeclaredType) t).asElement().getSimpleName().toString();

            messager.printMessage(Diagnostic.Kind.NOTE, "source interface include target: "+name);

            if(types.isSameType(t, elements.getTypeElement(target).asType()))
                return true;
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : annotations) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }
}
