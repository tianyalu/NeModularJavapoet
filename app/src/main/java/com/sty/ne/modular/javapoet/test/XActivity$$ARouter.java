package com.sty.ne.modular.javapoet.test;


import com.sty.ne.modular.javapoet.MainActivity;

/**
 * @Author: tian
 * @UpdateDate: 2020/10/12 9:48 PM
 */
public class XActivity$$ARouter {
    public static Class<?> findTargetClass(String path) {
//        if(path.equalsIgnoreCase("app/MainActivity")) {
//            return MainActivity.class;
//        }
//        return null;
        return path.equalsIgnoreCase("/app/MainActivity") ? MainActivity.class : null;
    }
}
