# Salvage
A annotation-processing based library to salvage your view state across configuration changes in a quick and efficient way. It is Kotlin-first, meaning this project is compiled mostly with Kotlin and is tested first on Kotlin.

## Getting Started

Add the Jitpack.io repo to your project:
```groovy
allProjects {
  repositories {
    // required to find the project's artifacts
    maven { url "https://www.jitpack.io" }
  }
}
```

Add the the artifacts to the project-level build.gradle:

```

def salvage_version = "1.0.2"

dependencies {

  annotationProcessor "com.github.fuzz-productions.Salvage:salvage-processor:${salvage_version}"
  compile "com.github.fuzz-productions.Salvage:salvage-core:${salvage_version}"
  compile "com.github.fuzz-productions.Salvage:salvage:${salvage_version}"

}

```

## Proguard

Proguard configuration is:

```
-keep class * extends com.fuzz.android.salvage.BundlePersister { *; }
```
Since we use reflection to instantiate persisters as we use them, we need to ensure
the class is kept around.

## How To Use

Simple as annotating your class:

```kotlin

// must provide visible default constructor
@Persist
data class User(var name: String? = null, var age: Int = 0)

```

or in Java
```java

// must provide visible default constructor
@Persist
public class User {

    private String name;

    private int age;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
}

```

Then in your `Fragment`, `Activity`, or other class that uses `Bundle` states:

```kotlin

    private var user: User? = null

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        Salvager.onSaveInstanceState(user, bundle)
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle);
        // restore, if user not null, we reuse the object to not unnecessarily
        // recreate instance. If null, we create a new instance
        user = Salvager.onRestoreInstanceState(user, bundle)

        // do something with restored state
      }
```

## Supported Types:

1. Serializable
2. All primitive + boxed types, including nullable + not null Kotlin Types that Android's `Bundle` supports
3. Nested `@Persist` objects
3. `List` of all these kinds, `Map<K,V>` with any type of key or value. `Map<K, List<T>>` is not supported yet.
4. We also support private fields (with getter/setters), package private fields in same package (or not via `PersistHelper`), and public
5. Inherited fields are collected as well.

## Advanced Features

### Ignoring Fields

If you have fields you wish to exclude from `@Persist`, use the `@PersistIgnore`:

```
@PersistIgnore
var user: User? = null
```

### Custom BundlePersister

To change how a field is persisted + restored, create a custom `BundlePersister`:

```kotlin

class CustomStringPersister : BundlePersister<String> {
    override fun persist(obj: String?, bundle: Bundle, uniqueBaseKey: String) {
        bundle.putString(uniqueBaseKey + "candy", obj)
    }

    override fun unpack(`object`: String?, bundle: Bundle, uniqueBaseKey: String): String {
        return bundle.getString(uniqueBaseKey + "candy")
    }
}

```

Then register it on the field you need:

```kotlin

@PersistField(bundlePersister = CustomStringPersister::class)
var name: String = ""

```

### Field Detection

By default, `Salvage` looks for all fields (and in all parent classes) that are:
  1. public
  2. package private
  3. private (with both getter / setter specified as bean)

Also they may be `val` (`final` in Java) if you use a nested `@Persist` or
custom `BundlePersister` to reuse the same instance if you wish to instantiate
the instance yourself.

`Salvage` ignores:
  1. `transient`
  2. `private` fields missing a getter or setter.
  3. Fields annotated with `@PersistIgnore`
  4. `static` fields (`@JvmStatic`)

#### Persist Policy

Instead of having to manually add `@PersistIgnore` or get stuck when you subclass an external object that you cannot control, you can tweak the `PersistPolicy`.

  `VISIBLE_FIELDS_AND_METHODS`: Default lookup mechanism.

  `VISIBLE_FIELDS_ONLY`: package private or public fields only

  `ANNOTATIONS_ONLY` : explicity specify `@PersistField`

  `PRIVATE_ACCESSORS_ONLY`: any private fields that have accessors

## Maintainer
[agrosner](https://github.com/agrosner) ([@agrosner](https://www.twitter.com/agrosner))
