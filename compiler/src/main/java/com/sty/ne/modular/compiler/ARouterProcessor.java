package com.sty.ne.modular.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.sty.ne.modular.annotation.ARouter;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @Author: tian
 * @UpdateDate: 2020/10/13 9:21 PM
 */
//通过AutoService来自动生成注解处理器，用来做注册，类似在Manifest中注册Activity
//build/classes/java/main/META-INF/services/javax.annotation.processing.Processor
@AutoService(Processor.class)
//该注解处理器需要处理哪一种注解的类型
@SupportedAnnotationTypes("com.sty.ne.annotation.ARouter")
//需要用什么样的JDK版本来编译，来进行文件的生成
@SupportedSourceVersion(SourceVersion.RELEASE_7)
//注解处理器能够接受的参数  在app 的build.gradle文件中配置
@SupportedOptions("content")
public class ARouterProcessor extends AbstractProcessor {
    //操作Element工具类
    private Elements elementsUtils;
    //type(类信息) 工具类
    private Types typesUtils;
    //用来输出警告、错误等日志
    private Messager messager;
    //文件生成器
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementsUtils = processingEnv.getElementUtils();
        typesUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();

        String content = processingEnv.getOptions().get("content");
        messager.printMessage(Diagnostic.Kind.NOTE, content);
    }

    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成Java文件
     * @param set 使用了支持处理注解的节点集合（类上面写了注解）
     * @param roundEnvironment 当前或是之前的运行环境，可以通过该对象查找找到的注解
     * @return true 表示后续处理器不会再处理（已经处理完成）
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if(set.isEmpty()) {
            return false;
        }
        //获取所有被 @ARouter 注解的类节点
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
        //遍历所有的类节点
        for (Element element : elements) {
            //类节点上一个节点：包节点
            String packageName = elementsUtils.getPackageOf(element).getQualifiedName().toString();
            //获取简单的类名
            String className = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "被 @ARouter注解的类有: " + className);
            //最终生成的类文件名，如：MainActivity$$ARouter
            String finalClassName = className + "$$ARouter";

            ARouter aRouter = element.getAnnotation(ARouter.class);

            //public static Class<?> findTargetClass(String path) {
            MethodSpec methodSpec = MethodSpec.methodBuilder("findTargetClass")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(Class.class)
                    .addParameter(String.class, "path")
                    //return path.equalsIgnoreCase("/app/MainActivity") ? MainActivity.class : null;
                    .addStatement("return path.equalsIgnoreCase($S) ? $T.class : null",
                            aRouter.path(),
                            ClassName.get((TypeElement) element))
                    .build();

            TypeSpec typeSpec = TypeSpec.classBuilder(finalClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(methodSpec)
                    .build();

            JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .build();

            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
