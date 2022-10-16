package com.llw.annotation;

import android.app.Activity;

public class CustomKnife {

    public static void bind(Activity activity) {
        String name = activity.getClass().getName() + "_ViewBinding";
        try {
            //通过反射生成一个类对象
            Class<?> aClass = Class.forName(name);
            //通过newInstance得到接口实例
            IBinder iBinder = (IBinder) aClass.newInstance();
            //最后调用接口bind()方法
            iBinder.bind(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
