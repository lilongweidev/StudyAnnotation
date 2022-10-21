package com.llw.apt_processor;

import com.google.auto.service.AutoService;
import com.llw.apt_annotation.BindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

/**
 * 注解处理器，用来生成代码的
 * 使用前需要注册
 */
@AutoService(Processor.class)
public class AnnotationsProcessor extends AbstractProcessor {

    // 定义用来生成APT目录下面的文件的对象（例如：MainActivity_ViewBinding）
    Filer filer;

    /**
     * 支持版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 支持的注解类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }

    /**
     * 初始化
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }


    /**
     * 通过注解处理器处理注解，生成版本代码到build文件夹中
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获取App中使用了BindView注解的对象
        Set<? extends Element> elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(BindView.class);

        //开始对elementsAnnotatedWith进行分类
        Map<String, List<VariableElement>> map = new HashMap<>();
        for (Element element : elementsAnnotatedWith) {
            //获取所注解的变量元素
            VariableElement variableElement = (VariableElement) element;
            //通过变量元素获取所处的外部类（例如MainActivity中的TextView，TextView就是变量元素，MainActivity就是外部类）
            String activityName = variableElement.getEnclosingElement().getSimpleName().toString();
            //获取map集合中的变量元素列表，如果为空则new一个，再添加进入map集合。
            List<VariableElement> variableElements = map.get(activityName);
            if (variableElements == null) {
                variableElements = new ArrayList<>();
                map.put(activityName, variableElements);
            }
            //添加到变量元素列表中
            variableElements.add(variableElement);
        }

        //makefile(map);
        makefilePlus(map);
        return false;
    }

    /**
     * 通过javapoet方式生成编译时类
     */
    private void makefilePlus(Map<String, List<VariableElement>> map) {
        if (map.size() > 0) {
            for (String activityName : map.keySet()) {
                //通过键获取到值 （值：变量元素列表）
                List<VariableElement> variableElements = map.get(activityName);
                //获取变量元素所处的外部类，这里强转一下
                TypeElement enclosingElement = (TypeElement) variableElements.get(0).getEnclosingElement();
                //得到包名
                String packageName = processingEnv.getElementUtils().getPackageOf(enclosingElement).toString();
                //获取类名实例方式一
                ClassName iBinderName = ClassName.get(packageName, "IBinder");
                //获取类名实例方式二
                ClassName activityClassName = ClassName.bestGuess(activityName);
                //创建类构造器，例如MainActivity_ViewBinding
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(activityName + "_ViewBinding")
                        //添加修饰符 public
                        .addModifiers(Modifier.PUBLIC)
                        //添加实现接口，例如 implements IBinder<MainActivity>
                        .addSuperinterface(ParameterizedTypeName.get(iBinderName, activityClassName));
                //创建方法构造器 方法名bind()
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bind")
                        //添加注解
                        .addAnnotation(Override.class)
                        //添加修饰符
                        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                        //添加方法参数
                        .addParameter(activityClassName, "target");

                for (VariableElement variableElement : variableElements) {
                    //获取控件名称
                    String variableName = variableElement.getSimpleName().toString();
                    //通过注解拿到控件的Id
                    int id = variableElement.getAnnotation(BindView.class).value();
                    //在方法中添加代码，写findViewById语句（例如：target.tvText = target.findViewById(2131231127);）
                    //$L代表的是字面量，variableName对应第一个L，i对应第二个L
                    methodBuilder.addStatement("target.$L = target.findViewById($L)", variableName, id);
                }
                //添加方法
                classBuilder.addMethod(methodBuilder.build());
                try {
                    //写入文件
                    JavaFile.builder(packageName, classBuilder.build())
                            .build()
                            .writeTo(filer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 通过手动拼写的方式生成编译时类
     */
    private void makefile(Map<String, List<VariableElement>> map) {
        //生成文件
        if (map.size() > 0) {
            //创建输入流
            Writer writer = null;
            //获取map集合的迭代器
            Iterator<String> iterator = map.keySet().iterator();
            //如果iterator.hasNext()为true，执行循环体中的代码
            while (iterator.hasNext()) {
                //获取map的键 （键：外部类名称）
                String activityName = iterator.next();
                //通过键获取到值 （值：变量元素列表）
                List<VariableElement> variableElements = map.get(activityName);
                //获取变量元素所处的外部类，这里强转一下
                TypeElement enclosingElement = (TypeElement) variableElements.get(0).getEnclosingElement();
                //得到包名
                String packageName = processingEnv.getElementUtils().getPackageOf(enclosingElement).toString();
                //准备写文件，要处理异常
                try {
                    //创建源文件
                    JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + activityName + "_ViewBinding");
                    //打开文件输入流并赋值给writer
                    writer = sourceFile.openWriter();
                    //写入包名（例如：package com.llw.annotation;）
                    writer.write("package " + packageName + ";\n");
                    //写入导包IBinder（例如：import com.llw.annotation.IBinder;）
                    writer.write("import " + packageName + ".IBinder;\n");
                    //换行
                    writer.write("\n");
                    //写入类实现IBinder接口（例如：public class MainActivity_ViewBinding implements
                    // IBinder<com.llw.annotation.MainActivity>{）
                    writer.write("public class " + activityName + "_ViewBinding implements IBinder<" + packageName + "." + activityName + "> {\n");
                    //写入@Override注解，注意格式（例如：@Override）
                    writer.write("\n    @Override");
                    //写入bind方法（例如：public void bind(com.llw.annotation.MainActivity target) {）
                    writer.write("\n    public void bind(" + packageName + "." + activityName + " target) {\n");
                    //遍历类中每一个需要findViewById的控件
                    for (VariableElement variableElement : variableElements) {
                        //获取控件名称
                        String variableName = variableElement.getSimpleName().toString();
                        //通过注解拿到控件的Id
                        int id = variableElement.getAnnotation(BindView.class).value();
                        //获取控件类型
                        TypeMirror typeMirror = variableElement.asType();
                        //写findViewById语句（例如：target.tvText = (android.widget.TextView) target.findViewById(2131231127);）
                        writer.write("        target." + variableName + " = (" + typeMirror + ") target.findViewById(" + id + ");");
                    }
                    //换行 结束
                    writer.write("\n    }\n}");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}












