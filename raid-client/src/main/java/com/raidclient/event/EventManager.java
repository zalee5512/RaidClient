package com.raidclient.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventManager {
    private static class MethodData {
        public final Object source;
        public final Method target;

        public MethodData(Object source, Method target) {
            this.source = source;
            this.target = target;
        }
    }

    private final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<>();

    public void register(Object o) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class) && method.getParameterTypes().length == 1) {
                method.setAccessible(true);
                Class<?> eventClass = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(eventClass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> casted = (Class<? extends Event>) eventClass;
                    REGISTRY_MAP.computeIfAbsent(casted, k -> new ArrayList<>()).add(new MethodData(o, method));
                }
            }
        }
    }

    public void unregister(Object o) {
        for (List<MethodData> list : REGISTRY_MAP.values()) {
            list.removeIf(data -> data.source.equals(o));
        }
    }

    public void call(Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());
        if (dataList != null) {
            for (MethodData data : dataList) {
                try {
                    data.target.invoke(data.source, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
