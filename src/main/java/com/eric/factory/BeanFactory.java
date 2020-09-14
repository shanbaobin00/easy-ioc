package com.eric.factory;

import com.eric.utils.ClassPathXmlParser;
import com.eric.utils.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象

    static {
        // 将解析xml的逻辑放置在ClassPathXmlParser解析类中
        Parser classPathXmlParser = new ClassPathXmlParser();
        classPathXmlParser.parse(map, "beans.xml");
    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

    /**
     * 根据类型从容器中获取bean
     * @param type
     * @return
     */
    public static List getBeanByType(Class<?> type){
        List list = new ArrayList();
        for(Map.Entry<String, Object> entry : map.entrySet()){
            if (type.isInstance(entry.getValue())){
                list.add(entry.getValue());
            }
        }
        return list;
    }

}
