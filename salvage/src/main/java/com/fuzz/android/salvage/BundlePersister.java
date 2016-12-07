package com.fuzz.android.salvage;

import android.os.Bundle;

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

public interface BundlePersister<T> {

    void persist(T obj, Bundle bundle);

    void unpack(T object, Bundle bundle);
}
