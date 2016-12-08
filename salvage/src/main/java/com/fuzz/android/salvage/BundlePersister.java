package com.fuzz.android.salvage;

import android.os.Bundle;

/**
 * Description: The main interface by which objects get persisted. Implement this class
 * to provide your own implementation for persistence.
 *
 * @author Andrew Grosner (Fuzz)
 */
public interface BundlePersister<T> {

    void persist(T obj, Bundle bundle, String uniqueBaseKey);

    void unpack(T object, Bundle bundle, String uniqueBaseKey);
}
