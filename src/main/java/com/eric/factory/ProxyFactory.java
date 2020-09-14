package com.eric.factory;

import com.eric.annotation.Autowired;
import com.eric.annotation.Service;
import com.eric.annotation.Transactional;
import com.eric.utils.TransactionManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author 应癫
 *
 *
 * 代理对象工厂：生成代理对象的
 */
@Service("proxyFactory")
public class ProxyFactory {

    @Autowired
    private TransactionManager transactionManager;

//    public void setTransactionManager(TransactionManager transactionManager) {
//        this.transactionManager = transactionManager;
//    }

    /*private ProxyFactory(){

    }

    private static ProxyFactory proxyFactory = new ProxyFactory();

    public static ProxyFactory getInstance() {
        return proxyFactory;
    }*/



    /**
     * Jdk动态代理
     * @param obj  委托对象
     * @return   代理对象
     */
    public Object getJdkProxy(Object obj) {

        // 获取代理对象
        return  Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;

                        // 注意这里拿到的method是接口的，需要拿到真正被代理类的method
                        Method oldMethod = obj.getClass().getMethod(method.getName(), method.getParameterTypes());
                        // 进行过滤，只对@Transactional修饰的方法进行增强
                        Transactional transactional = oldMethod.getAnnotation(Transactional.class);
                        if (transactional == null){
                            return method.invoke(obj,args);
                        }

                        try{
                            // 开启事务(关闭事务的自动提交)
                            transactionManager.beginTransaction();

                            result = method.invoke(obj,args);

                            // 提交事务

                            transactionManager.commit();
                        }catch (Exception e) {
                            e.printStackTrace();
                            // 回滚事务
                            transactionManager.rollback();

                            // 抛出异常便于上层servlet捕获
                            throw e;

                        }

                        return result;
                    }
                });

    }


    /**
     * 使用cglib动态代理生成代理对象
     * @param obj 委托对象
     * @return
     */
    public Object getCglibProxy(Object obj) {
        return  Enhancer.create(obj.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                Object result = null;

                // 进行过滤，只对@Transactional修饰的方法进行增强
                Transactional transactional = method.getAnnotation(Transactional.class);
                if (transactional == null){
                    return method.invoke(obj,objects);
                }

                try{
                    // 开启事务(关闭事务的自动提交)
                    transactionManager.beginTransaction();

                    result = method.invoke(obj,objects);

                    // 提交事务

                    transactionManager.commit();
                }catch (Exception e) {
                    e.printStackTrace();
                    // 回滚事务
                    transactionManager.rollback();

                    // 抛出异常便于上层servlet捕获
                    throw e;

                }
                return result;
            }
        });
    }
}
