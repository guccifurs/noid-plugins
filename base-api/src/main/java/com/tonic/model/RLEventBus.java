package com.tonic.model;

import com.tonic.Logger;
import com.tonic.util.ReflectUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

public class RLEventBus
{
    @Getter
    private final Object eventBus;
    private Method postMethod;

    RLEventBus(Guice injector) {
        this.eventBus = injector.getBinding("net.runelite.client.eventbus.EventBus");
    }

    public void post(Object event) {
        try
        {
            if(postMethod == null)
            {
                getPostMethod();
            }
            postMethod.invoke(eventBus, event);
        }
        catch (Exception e)
        {
            Logger.error("Failed to post event: " + event.getClass().getName());
        }
    }

    @SneakyThrows
    private void getPostMethod() {
        Method method = eventBus.getClass().getDeclaredMethod("post", Object.class);
        method.setAccessible(true);
        this.postMethod = method;
    }

    public void register(Object listener) {
        try
        {
            ReflectUtil.getMethod(eventBus, "register", new Class[]{Object.class}, new Object[]{listener});
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logger.error("Failed to register listener: " + listener.getClass().getName());
        }
    }

    public void unregister(Object listener) {
        try
        {
            // Prefer Object.class signature
            ReflectUtil.getMethod(eventBus, "unregister", new Class[]{Object.class}, new Object[]{listener});
        }
        catch (Exception e1)
        {
            try
            {
                // Fallback to concrete class signature used by some implementations
                Class<?> listenerClass = listener.getClass();
                ReflectUtil.getMethod(eventBus, "unregister", new Class[]{listenerClass}, new Object[]{listener});
            }
            catch (Exception ignored)
            {
                // Swallow to avoid noisy logs when listeners are already unregistered or not registered
            }
        }
    }
}
