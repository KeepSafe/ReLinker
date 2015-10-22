# ReLinker 

[![Release](https://img.shields.io/github/release/KeepSafe/ReLinker.svg?label=maven)](https://jitpack.io/#KeepSafe/ReLinker)

A robust native library loader for Android.

## Overview

The Android `PackageManager`'s native library loading is unreliable. Occasionally when using native libraries, you will encounter a stack trace like this:

```
java.lang.UnsatisfiedLinkError: Couldn't load stlport_shared from loader dalvik.system.PathClassLoader: findLibrary returned null
at java.lang.Runtime.loadLibrary(Runtime.java:365)
at java.lang.System.loadLibrary(System.java:535)
at com.kii.safe.Native.<clinit>(Native.java:16)
... 63 more

Caused by: java.lang.UnsatisfiedLinkError: Library stlport_shared not found
at java.lang.Runtime.loadLibrary(Runtime.java:461)
at java.lang.System.loadLibrary(System.java:557)
at com.kii.safe.Native.<clinit>(Native.java:16)
... 5 more
```

ReLinker fixes these issues by replacing the standard `System.loadLibrary` call with a more reliable implementation.

Note that this library fixes intermittent link errors; if you get an error every time you use your app, you may have a configuration issue. See [this StackOverflow question](http://stackoverflow.com/questions/27421134/system-loadlibrary-couldnt-find-native-library-in-my-case) for more information.

## Installation

ReLinker is distributed using [jitpack](https://jitpack.io).

To use ReLinker in your project, add the following to your gradle build file:

```groovy
   repositories { 
        jcenter()
        maven { url "https://jitpack.io" }
   }
   
   dependencies {
         compile 'com.github.KeepSafe:ReLinker:1.0'
   }
```

## Usage

Simply replace a call to `System.loadLibrary` like this:

```java
System.loadLibrary("mylibrary");
```

With a call to `ReLinker.loadLibrary` like this:

```java
ReLinker.loadLibrary(context, "mylibrary");
```

## Sample application

See the sample application under `sample/` for a quick demo.


## License

    Copyright 2015 KeepSafe Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.