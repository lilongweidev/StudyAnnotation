package com.llw.apt_processor;

import com.google.auto.service.AutoService;
import com.llw.apt_annotation.BindView;

import java.io.IOException;
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
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

@AutoService(Process.class)
public class AnnotationProcessor extends AbstractProcessor {

    private Filer mFiler;

    /**
     * 注解处理器初始化
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
    }

    /**
     * 支持的版本
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

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获取使用了BindView注解的对象
        Set<? extends Element> elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(BindView.class);

        Map<String, List<VariableElement>> map = new HashMap<>();
        for (Element element : elementsAnnotatedWith) {
            //获取所注解的变量元素
            VariableElement variableElement = (VariableElement) element;
            //通过变量元素获取所处的外部类（例如MainActivity中的TextView，TextView就是变量元素，MainActivity就是外部类）
            String outerClass = variableElement.getEnclosingElement().getSimpleName().toString();
            //获取map中的变量元素列表，如果为空则new一个，再添加进入map集合。
            List<VariableElement> variableElementList = map.get(outerClass);
            if (variableElementList == null) {
                variableElementList = new ArrayList<>();
                map.put(outerClass, variableElementList);
            }
            //添加到变量元素列表中
            variableElementList.add(variableElement);
        }

        //生成文件
        if (map.size() > 0) {
            //创建输入流
            Writer writer = null;
            //获取map集合的迭代器
            Iterator<String> iterator = map.keySet().iterator();
            //如果iterator.hasNext()为true，执行循环体中的代码
            while (iterator.hasNext()) {
                //获取map的键 （键：外部类名称）
                String outerClass = iterator.next();
                //通过键获取到值 （值：变量元素列表）
                List<VariableElement> variableElements = map.get(outerClass);
                //获取变量元素所处的外部类，这里强转一下
                TypeElement enclosingElement = (TypeElement) variableElements.get(0).getEnclosingElement();
                //获取包名（通过AbstractProcessor的成员变量processingEnv获取ElementUtils工具类，再通过外部类获取所处的包的名字）
                String packageName = processingEnv.getElementUtils().getPackageOf(enclosingElement).toString();
                //准备写文件，要处理异常
                try {
                    //定义文件名 （包名 + 类名）
                    String fileName = packageName + "." + outerClass + "_ViewBinding";
                    //创建源文件
                    JavaFileObject sourceFile = mFiler.createSourceFile(fileName);
                    //打开文件输入流并赋值给writer
                    writer = sourceFile.openWriter();
                    //开始写文件，第一行包名
                    writer.write("package " + packageName + ";\n");
                    writer.write("import " + packageName + ".IBinder;\n");
                    writer.write("public class " + outerClass + "_ViewBinding implements IBinder<" +
                            packageName + "." + outerClass + ">{\n");
                    writer.write("    @Override\n" +
                            "    public void bind(" + packageName + "." + outerClass + " target){");
                    //遍历类中每一个需要findViewById的控件
                    for (VariableElement variableElement : variableElements) {
                        //获取控件名称
                        String variableName = variableElement.getSimpleName().toString();
                        //通过注解拿到控件的Id
                        int id = variableElement.getAnnotation(BindView.class).value();
                        //获取控件类型
                        TypeMirror typeMirror = variableElement.asType();
                        //写findViewById语句
                        writer.write("        target." + variableName +
                                " = (" + typeMirror + ") target.findViewById(" + id + ");\n");
                    }
                    writer.write("        }\n");
                    writer.write("    }\n");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try{
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return false;
    }
}
