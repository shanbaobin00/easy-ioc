package com.eric.utils;

import java.util.Map;

/**
 * @Author: shanbb
 * @Date: 2020/9/7 10:42
 * @Description:
 * @Modified By:
 */
public interface Parser {
    void parse(Map<String,Object> map, String path);
}
