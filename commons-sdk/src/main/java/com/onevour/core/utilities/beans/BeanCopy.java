package com.onevour.core.utilities.beans;

import android.util.Log;

import com.onevour.core.utilities.commons.ValueOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"CollectionAddAllCanBeReplacedWithConstructor", "unchecked", "rawtypes"})
public class BeanCopy {

    private static final String TAG = BeanCopy.class.getSimpleName();

    private static final Map<String, Set<String>> cached = new HashMap<>();

    public static <S, T> T value(S source, Class<T> target, String... ignore) {
        if (ValueOf.isNull(source)) throw new NullPointerException();
        try {
            Constructor constructor = target.getConstructor();
            T newInstance = (T) constructor.newInstance();
            copyValue(source, newInstance, ignore);
            return newInstance;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            Log.e(TAG, "error copy value " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static <S, T> List<T> values(List<S> source, Class<T> target, String... ignore) {
        List<T> result = new ArrayList<>();
        if (ValueOf.isNull(source)) return result;
        if (source.isEmpty()) return result;
        for (Object o : source) {
            result.add(value(o, target, ignore));
        }
        return result;
    }

    public static <S, T> void copyValue(S source, T target, String... ignore) {
        Set<String> ignoreSet = new HashSet<>();
        ignoreSet.addAll(Arrays.asList(ignore));
        Class<?> clazzSource = source.getClass();
        Class<?> clazzTarget = target.getClass();

        // copy name
        StringBuilder sb = new StringBuilder();
        sb.append(clazzSource.getSimpleName());
        sb.append("To");
        sb.append(clazzTarget.getSimpleName());
        String name = sb.toString();
        Set<String> sets = cached.get(name);
        if (Objects.isNull(sets)) {
            sets = new HashSet<>();
            List<Field> fieldSources = getAllModelFields(clazzSource);
            List<Field> fieldTargets = getAllModelFields(clazzTarget);
            for (Field field : fieldSources) {
                // ignore field
                if (ignoreSet.contains(field.getName())) continue;
                // ignore name and type if not match
                for (Field fieldTarget : fieldTargets) {
                    if (!field.getName().equalsIgnoreCase(fieldTarget.getName())) continue;
                    if (!field.getType().equals(fieldTarget.getType())) continue;
                    try {
                        field.setAccessible(true);
                        fieldTarget.setAccessible(true);
                        fieldTarget.set(target, field.get(source));
                        sets.add(fieldTarget.getName());
                        break;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            cached.put(name, sets);
        } else {
            assert sets != null;
            for (String fieldName : sets) {
                try {
                    Field fieldSource = clazzSource.getDeclaredField(fieldName);
                    Field fieldTarget = clazzTarget.getDeclaredField(fieldName);
                    fieldSource.setAccessible(true);
                    fieldTarget.setAccessible(true);
                    fieldTarget.set(target, fieldSource.get(source));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected static List<Field> getAllModelFields(Class aClass) {
        List<Field> fields = new ArrayList<>();
        do {
            Collections.addAll(fields, aClass.getDeclaredFields());
            aClass = aClass.getSuperclass();
        } while (aClass != null);
        return fields;
    }
}
