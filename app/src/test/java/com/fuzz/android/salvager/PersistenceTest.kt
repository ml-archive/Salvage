package com.fuzz.android.salvager

import android.os.Bundle
import com.fuzz.android.salvage.Salvager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
@RunWith(RobolectricTestRunner::class)
class PersistenceTest {

    @Test
    fun testCanPersistExample() {

        val example = Example("Andrew", 25)

        val bundle = Bundle()
        Salvager.onSaveInstanceState(example, bundle)

        val exampleRestored = Example("", 0)
        Salvager.onRestoreInstanceState(exampleRestored, bundle)

        assertEquals("Andrew", exampleRestored.name)
        assertEquals(25, exampleRestored.age)
    }

    @Test
    fun testCanPersistList() {

        val listExample = ListExample(arrayListOf(ParentObject(Example("Andrew", 20))),
                arrayListOf("Yellow"), arrayListOf(SimpleSerializable()))

        val bundle = Bundle()
        Salvager.onSaveInstanceState(listExample, bundle)

        val listRestored = ListExample()
        Salvager.onRestoreInstanceState(listRestored, bundle)

        val parentList = listRestored.list
        assertNotNull(parentList)
        assertEquals(1, parentList.size)
    }
}