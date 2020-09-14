package com.eric.utils;

import com.eric.annotation.Autowired;
import com.eric.annotation.Transactional;
import com.eric.annotation.Service;
import com.eric.factory.BeanFactory;
import com.eric.factory.ProxyFactory;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * @Author: shanbb
 * @Date: 2020/9/7 10:41
 * @Description:
 * @Modified By:
 */
public class AnnotationParser implements Parser {

    @Override
    public void parse(Map<String, Object> map, String path) {
        Set<Class<?>> classes = new HashSet<>();
        String packageDirName = path.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                // 获取包的物理路径
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                findAndAddClassesInPackageByFile(path, filePath, true, classes);
            }
            System.out.println("开始实例化bean");
            // 1、将指定包名下的所有@Service注解的类实例化并加入容器
            createInstances(map, classes);

            System.out.println("开始对bean进行事务增强");
            // 2、最后对单例池中的bean进行事务增强
            transactionEnhance(map);

            System.out.println("开始对bean进行依赖注入");
            // 3、对单例池中的bean进行类型依赖注入
            dependencyInject(map);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 事务增强处理
     * @param map
     */
    private void transactionEnhance(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object parentObject = entry.getValue();
            Method[] methods = parentObject.getClass().getMethods();

            for (Method method : methods) {
                Transactional transactional = method.getAnnotation(Transactional.class);
                if (transactional != null){
                    // 该方法声明了事务，需要进行增强处理

                    // 获取事务代理工厂实例对象
                    ProxyFactory proxyFactory = (ProxyFactory) map.get("proxyFactory");

                    // 注意在增强的时候我们需要看增强的类是否实现了接口
                    // 从而选择JDK和CGLIB动态代理
                    Class<?>[] interfaces = parentObject.getClass().getInterfaces();
                    if (interfaces == null || interfaces.length == 0){
                        // CGLIB
                        map.put(entry.getKey(), proxyFactory.getCglibProxy(parentObject));
                    }else{
                        // JDK
                        map.put(entry.getKey(), proxyFactory.getJdkProxy(parentObject));
                    }
                }
            }
        }
    }

    /**
     * 针对@Service注解修饰的类进行实例化并加入单例池中
     * @param map
     * @param classes
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void createInstances(Map<String, Object> map, Set<Class<?>> classes) throws InstantiationException, IllegalAccessException {
        for (Class<?> aClass : classes) {
            Service service = aClass.getAnnotation(Service.class);
            if (service == null) {
                continue;
            }
            // 构建beanId
            String beanId = null;
            if ("".equals(service.value())) {
                beanId = aClass.getSimpleName();
            } else {
                beanId = service.value();
            }
            // 实例化bean
            Object o = aClass.newInstance();
            // 将实例化后的bean放入单例池中
            map.put(beanId, o);
        }
    }

    /**
     * 依赖注入
     * @param map
     * @throws Exception
     */
    private void dependencyInject(Map<String, Object> map) throws Exception {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object parentObject = entry.getValue();
            Field[] fields = parentObject.getClass().getDeclaredFields();
            for (Field field : fields) {
                Autowired autowired = field.getAnnotation(Autowired.class);
                if (autowired != null) {
                    // 需要进行注入操作
                    List beans = BeanFactory.getBeanByType(field.getType());
                    if (beans.size() == 0) {
                        throw new Exception("DI Error, can not found this type: " + field.getType());
                    }
                    if (beans.size() > 1) {
                        throw new Exception("DI Error, found multiple beans in this type: " + field.getType());
                    }
                    field.setAccessible(true);
                    field.set(parentObject, beans.get(0));
                }
            }
            map.put(entry.getKey(), parentObject);
        }
    }


    /**
     * 查询class保存到set集合中
     *
     * @param packageName 包名称
     * @param packagePath 包路径
     * @param recursive   是否递归
     * @param classes     class集合
     */
    public static void findAndAddClassesInPackageByFile(String packageName,
                                                        String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            // log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    //classes.add(Class.forName(packageName + '.' + className));
                    //经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }

}
