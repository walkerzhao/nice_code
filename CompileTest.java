package com.zy;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * 动态编译 测试
 * @author andy
 *
 */
public class CompileTest {
	public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, MalformedURLException {
		System.out.println("hello,world");
		

        //动态编译磁盘中的代码
        //生成的字节码文件存放到<module>/build/classes/main目录下
        File distDir = new File("proto");
        if (!distDir.exists()) {
            distDir.mkdirs();
        }
        System.out.println("distDir:"+distDir.getAbsolutePath());
        File javaFile = new File("proto/Hello.java");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        //JavaCompiler最核心的方法是run, 通过这个方法编译java源文件, 前3个参数传null时, 
        //分别使用标准输入/输出/错误流来 处理输入和编译输出. 使用编译参数-d指定字节码输出目录.
        int compileResult = javac.run(null, null, null, "-d", distDir.getAbsolutePath(), javaFile.getAbsolutePath());
        //run方法的返回值: 0-表示编译成功, 否则表示编译失败
        if(compileResult != 0) {
            System.err.println("compile suc!!");
            return;
        } else {
        	System.out.println("compile suc");
        }
        
      //动态执行 (反射执行)
        URL url = distDir.toURI().toURL();  
        URL[] urls = new URL[]{url};  
         // Create a new class loader with the directory  
        ClassLoader cl = new URLClassLoader(urls);  
         // Load in the class; Test2.class should be located in  
         // the directory file:/D：\test\zy\  
        Class cls = cl.loadClass("Hello"); 
        System.out.println("cls path:"+cls.getName());
//        Class klass = Class.forName("com.zy.Hello");
        Method evalMethod = cls.getDeclaredMethod("main", String[].class);
        String[] args1={null};
        String result = (String)evalMethod.invoke(cls.newInstance(),args1);
        System.out.println("eval:" + result);
	}

}
