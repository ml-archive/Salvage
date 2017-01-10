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

## Proguard

Proguard configuration is:

```
-keep class * extends com.fuzz.android.salvage.BundlePersister { *; }
```
Since we use reflection to instantiate persisters as we use them, we need to ensure
the class is kept around.

## Maintainer
[agrosner](https://github.com/agrosner) ([@agrosner](https://www.twitter.com/agrosner))
