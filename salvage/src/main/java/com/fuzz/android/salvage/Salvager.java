package com.fuzz.android.salvage;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

@SuppressWarnings("unchecked")
public class Salvager {

    private static final Map<Class, BundlePersister> persisterMap = new HashMap<>();

    public static <T> BundlePersister<T> getBundlePersister(Class<T> tClass) {
        BundlePersister persister = persisterMap.get(tClass);
        if (persister == null) {
            try {
                persister = (BundlePersister) Class.forName(tClass.getName() + "Persister")
                        .newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not find generated BundlePersister for: $clazz. Ensure " +
                        "you specified the @Persist annotation.");
            }
            if (persister != null) {
                persisterMap.put(tClass, persister);
            }
        }
        return persister;
    }

    public static <T> void onSaveInstanceState(T obj, Bundle bundle) {
        if (obj == null || bundle == null) {
            return;
        }
        ((BundlePersister<T>) getBundlePersister(obj.getClass())).persist(obj, bundle);
    }

    public static <T> void onRestoreInstanceState(T obj, Bundle bundle) {
        if (obj == null || bundle == null) {
            return;
        }
        ((BundlePersister<T>) getBundlePersister(obj.getClass())).unpack(obj, bundle);
    }
}
