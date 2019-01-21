package ru.runa.wfe.web.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Object createInstanceClass(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return Class.forName(className).newInstance();
    }

    public static Object invokeMethod(Object object, String methodName, Object[] params, Class... nameParams) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = object.getClass().getMethod(methodName, nameParams);

        return method.invoke(object, params);
    }
}
