package com.tonic.model;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Guice
{
    private final Injector injector;
    private final Map<String,Object> BINDINGS;

    Guice(Injector injector) {
        this.injector = injector;
        this.BINDINGS = dumpBindings();
    }

    public Object getBinding(String key) {
        Object instance =  this.BINDINGS.get(key);
        if (instance != null) {
            return instance;
        }
        throw new IllegalArgumentException("No binding found for key: " + key);
    }

    private Map<String,Object> dumpBindings()
    {
        Map<String, Object> bindings = new HashMap<>();

        injector.getBindings().values().forEach(binding -> {
            Key<?> key = binding.getKey();
            Annotation annotation = key.getAnnotation();

            String mapKey = key.getTypeLiteral().getRawType().getName();

            if (annotation != null) {
                mapKey = ((Named) annotation).value();
            }

            bindings.put(mapKey, binding.getProvider().get());
        });

        return bindings;
    }
}
