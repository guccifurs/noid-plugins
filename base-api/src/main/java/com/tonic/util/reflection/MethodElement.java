package com.tonic.util.reflection;

import com.tonic.util.ReflectUtil;

public class MethodElement implements Element
{
    private final boolean isStatic;
    private final String name;
    private final Class<?>[] params;
    private final Object[] args;

    public MethodElement(boolean isStatic, String name, Class<?>[] params, Object[] args) {
        this.isStatic = isStatic;
        this.name = name;
        this.params = params == null ? new Class<?>[]{} : params;
        this.args = args == null ? new Object[]{} : args;
    }

    @Override
    public Object get(Object o) throws Exception {
        if(isStatic)
        {
            Class<?> clazz = o instanceof Class<?> ? (Class<?>)o : o.getClass();
            return ReflectUtil.getStaticMethod(clazz, name, params, args);
        }
        else
        {
            return ReflectUtil.getMethod(o, name, params, args);
        }
    }
}
