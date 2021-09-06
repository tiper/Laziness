# Laziness


[![](https://jitpack.io/v/tiper/Laziness.svg)](https://jitpack.io/#tiper/Laziness)
[![Kotlin Version](https://img.shields.io/badge/kotlin-1.3.61-blue.svg)](http://kotlinlang.org/)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Laziness, is a delegated property with context for Kotlin extensions. It calls the specified function with `this` value as its receiver and returns its result.

For more information please check:
- [Kotlin](https://kotlinlang.org)
- [Lazy](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/lazy.html)
- [Delegated Properties](https://kotlinlang.org/docs/reference/delegated-properties.html)
- [Run](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/run.html)

## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Then, add the library to your module `build.gradle`
```gradle
dependencies {
    implementation 'com.github.tiper:Laziness:latest.release.here'
}
```

## Usage

```kotlin
val MyClass.cachedStuff: Return? by laziness {
    doStuffToBeCached()
}
```

That's it!

License
--------

    Copyright 2019 Tiago Pereira

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
