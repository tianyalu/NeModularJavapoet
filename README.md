# 组件化APT高级用法JavaPoet

[TOC]

## 一、概念

### 1.1 什么是`JavaPoet`?

`APT` + `JavaPoet` = 超级利刃

`JavaPoet`是`square`推出的开源`java`代码生成框架，提供`Java API`生成`.java`源文件；这个框架功能非常实用，也是我们习惯的`Java`面向对象`OOP`语法；可以很方便地使用它根据注解生成对应代码，通过这中自动化生成代码的方式，可以让我们用更加简洁优雅的方式来替代繁琐冗杂的重复工作。

项目主页及源码地址：[https://github.com/square/javapoet](https://github.com/square/javapoet)

### 1.2 依赖`JavaPoet`库

`Android Studio 3.4.1` + `Gradle5.1.1` (向下兼容)

```groovy
// AS3.4.1 + Gradle5.1.1 + auto-service:1.0-rc4
annotationProcessor 'com.google.auto.service:auto-service:1.0-rc4'
compileOnly 'com.google.auto.service:auto-service:1.0-rc4'
```

依赖`JavaPoet`库

```groovy
//帮助我们通过类调用的形式来生成Java代码
implementation "com.squareup:javapoet:1.9.0"
```

### 1.3 `JavaPoet`8个常用的类 

> 1. `MethodSpec`：代表一个构造函数或方法声明；
> 2. `TypeSpec`：代表一个类、接口或者枚举声明；
> 3. `FieldSpec`：代表一个成员变量，一个字段声明；
> 4. `JavaFile`：包含一个顶级类的`Java`文件；
> 5. `ParameterSpec`：用来创建参数；
> 6. `AnnotationSpec`：用来创建注解；
> 7. `ClassName`：用来包装一个类；
> 8. `TypeName`：类型，如在添加返回值类型时使用`TypeName.VOID`。

### 1.4 `JavaPoet`字符串格式化规则

* `$L`：字面量，如：`"int value=$L", 10`；
* `$S`：字符串，如：`$S," hello"`；
* `$T`：类、接口，如：`$T,MainActivity`；
* `$N`：变量，如：`user.$N,name`。

## 二、实现 

### 2.1 实现思路

利用`APT`技术对于每个被`@ARouter`注解的类自动生成一个类似如下的`Class`文件：

```java
public class MainActivity$$ARouter {
    //public static Class<?> findTargetClass(String path) {
    //    if (path.equalsIgnoreCase("/app/MainActivity")) {
    //        return MainActivity.class;
    //    }
    //    return null;
    //}
  return path.equalsIgnoreCase("/app/MainActivity") ? MainActivity.class : null;
}
```

使用时直接通过路径参数调用`findTargetClass(String path)`方法即可找到对应的类名，从而实现`Activity`之间的跳转。

### 2.2 实现步骤

#### 2.2.1 `annotation`模块

`ARouter`注解声明文件：

```java
/**
 * <strong>Activity使用的布局文件注解</strong>
 * <ul>
 *  <li>@Target(ElementType.TYPE) //接口、类、枚举、注解</li>
 *  <li>@Target(ElementType.FIELD) //属性、枚举的常量</li>
 *  <li>@Target(ElementType.METHOD) //方法</li>
 *  <li>@Target(ElementType.PARAMETER) //方法参数</li>
 *  <li>@Target(ElementType.CONSTRUCTOR) //构造函数</li>
 *  <li>@Target(ElementType.LOCAL_VARIABLE) //局部变量</li>
 *  <li>@Target(ElementType.ANNOTATION_TYPE) //该注解使用在另一个注解上</li>
 *  <li>@Target(ElementType.PACKAGE) //包</li>
 *  <li>@Retention(RetentionPolicy.RUNTIME) //注解会在class字节码文件中存在，jvm加载时可以通过反射获取到该注解的内容</li>
 * </ul>
 *
 * 生命周期：SOURCE < CLASS < RUNTIME
 * 1. 一般如果需要在运行时去动态获取注解信息，用RUNTIME注解
 * 2. 要在编译时进行一些预处理操作，如ButterKnife，用CLASS注解，注解会在class文件中存在，但是在运行时会被丢弃
 * 3. 做一些检查性的操作，如@Override，用SOURCE源码注解，注解仅存在在源码级别，在编译的时候丢弃该注解
 * @Author: tian
 * @UpdateDate: 2020/10/12 8:48 PM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ARouter {
    //详细的路由路径（必填），如："/app/MainActivity"
    String path();

    //从path中截取出来，规范开发者的编码
    String group() default "";
}
```

#### 2.2.2 `compiler`模块

`build.gradle`文件：

```groovy
apply plugin: 'java-library'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // AS3.4.1 + Gradle5.1.1 + auto-service:1.0-rc4
    compileOnly 'com.google.auto.service:auto-service:1.0-rc4'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc4'

    //帮助我们通过类调用的形式来生成Java代码
    implementation "com.squareup:javapoet:1.9.0"

    //依赖注解
    implementation project(':annotation')
}

//java控制台输出中文乱码
tasks.withType(JavaCompile) {
    options.encoding = "UTF-8"
}

//JDK编译的版本
sourceCompatibility = "1.7"
targetCompatibility = "1.7"
```

注解处理器`ARouterProcessor`文件：

```java
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

```

### 2.2.3 `app`模块

`build.gradle`文件：

```groovy
android {   
  //...
	defaultConfig {
        applicationId "com.sty.ne.modular.javapoet"
        minSdkVersion 21
        targetSdkVersion 30
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        //在gradle文件中配置选项参数（用于APT传参接收）
        //切记：必须写在defaultConfig节点下
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [content : 'hello apt']
            }
        }
    }
  //...
}

dependencies {
		//...

    // 依赖注解
    implementation project(':annotation')
    // 注解处理器
    annotationProcessor project(':compiler')
}
```

`MainActivity`中使用：

```java
    public void jumpToOrder(View view) {
        Class<?> targetClass = OrderActivity$$ARouter.findTargetClass("/app/OrderActivity");
        startActivity(new Intent(this, targetClass));
    }
```