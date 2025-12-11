package com.tonic.util.reflection;

import com.tonic.util.ReflectUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FieldElement implements Element
{
    private final boolean isStatic;
    private final String name;

    @Override
    public Object get(Object o) throws Exception {
        if(isStatic)
        {
            Class<?> clazz = o instanceof Class ? (Class<?>) o : o.getClass();
            return ReflectUtil.getStaticField(clazz, name);
        }
        else
        {
            return ReflectUtil.getField(o, name);
        }
    }
}
