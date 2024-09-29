package com.peng.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:28
 */
public class ApplicationContextHolder {
    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (!checkApplication()) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String beanName) {
        if (!checkApplication()) {
            return null;
        }
        return (T) applicationContext.getBean(beanName);
    }

    private static Boolean checkApplication() {
        if (Objects.isNull(applicationContext)) {
            return false;
        }
        return true;
    }
}
