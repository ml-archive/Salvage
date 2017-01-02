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

        val listExample = ListExample(arrayListOf(ParentObject(Example("Andrew", 20)),
                ParentObject(Example("Andrew2", 25))),
                arrayListOf("Yellow"), arrayListOf(SimpleSerializable()))

        val bundle = Bundle()
        Salvager.onSaveInstanceState(listExample, bundle)

        val listRestored = ListExample()
        Salvager.onRestoreInstanceState(listRestored, bundle)

        val parentList = listRestored.list
        assertNotNull(parentList)
        assertEquals(2, parentList.size)

        var example = parentList[0].example
        assertNotNull(example)
        assertEquals("Andrew", example?.name)
        assertEquals(20, example?.age)

        example = parentList[1].example
        assertNotNull(example)
        assertEquals("Andrew2", example?.name)
        assertEquals(25, example?.age)

        val stringList = listRestored.listString
        assertNotNull(stringList)
        assertEquals(1, stringList.size)
        assertEquals("Yellow", stringList[0])

        val serializable = listExample.listSerializable
        assertEquals(1, serializable.size)
        assertNotNull(serializable[0])
    }

    @Test
    fun testCanFindInnerClass() {

        val inner = InnerClassExample.Inner()
        Salvager.onSaveInstanceState(inner, Bundle(), "")

        val restored = Salvager.onRestoreInstanceState(InnerClassExample.Inner::class.java, Bundle(), "")
        assertNotNull(restored)
    }
}