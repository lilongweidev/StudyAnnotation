package com.llw.annotation;

/**
 * 绑定Activity的接口
 * @param <T>
 */
public interface IBinder<T> {
    void bind(T target);
}
