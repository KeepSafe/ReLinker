<h1 align="center">
	<img src="https://raw.githubusercontent.com/KeepSafe/ReLinker/1.2/web/logo.png" width="256" height="256" alt="ReLinker"><br/>
	ReLinker
</h1>

[![Build Status](https://travis-ci.org/KeepSafe/ReLinker.svg?branch=master)](https://travis-ci.org/KeepSafe/ReLinker)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.getkeepsafe.relinker/relinker/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.getkeepsafe.relinker/relinker)
[![Release](https://img.shields.io/github/tag/KeepSafe/ReLinker.svg?label=jitpack)](https://jitpack.io/#KeepSafe/ReLinker)


A robust native library loader for Android. More information can be found in our [blog post](https://medium.com/keepsafe-engineering/the-perils-of-loading-native-libraries-on-android-befa49dce2db)

 **Min SDK:** 9

 [JavaDoc](https://jitpack.io/com/github/KeepSafe/Relinker/latest/javadoc/)

## Overview

The Android `PackageManager`'s native library loading is unreliable. Occasionally when using native libraries, you will encounter a stack trace like this:

```
java.lang.UnsatisfiedLinkError: Couldn't load stlport_shared from loader dalvik.system.PathClassLoader: findLibrary returned null
at java.lang.Runtime.loadLibrary(Runtime.java:365)
at java.lang.System.loadLibrary(System.java:535)
at com.your.app.NativeClass.<clinit>(Native.java:16)
... 63 more

Caused by: java.lang.UnsatisfiedLinkError: Library stlport_shared not found
at java.lang.Runtime.loadLibrary(Runtime.java:461)
at java.lang.System.loadLibrary(System.java:557)
at com.your.app.NativeClass.<clinit>(Native.java:16)
... 5 more
```

ReLinker fixes these issues by replacing the standard `System.loadLibrary` call with a more reliable implementation.

Note that this library fixes intermittent link errors; if you get an error every time you use your app, you may have a configuration issue. See [this StackOverflow question](http://stackoverflow.com/questions/27421134/system-loadlibrary-couldnt-find-native-library-in-my-case) for more information.

## Who needs ReLinker?

If your app includes native libraries, and your minimum SDK is below API 23 (Marshmallow), you need ReLinker.

There are a number of different bugs addressed by ReLinker; the last of these was resolved as of Marshmallow.  As long as your app's min SDK is at or above it, loading libraries via `System.loadLibrary("foo")` is safe.

## Installation

ReLinker is distributed using [MavenCentral](https://search.maven.org/artifact/com.getkeepsafe.relinker/relinker).

```groovy
   repositories { 
        mavenCentral()
   }
   
   dependencies {
         compile 'com.getkeepsafe.relinker:relinker:x.x.x'
   }
```

If you wish, you may also use ReLinker with [jitpack](https://jitpack.io/#KeepSafe/ReLinker)

## Usage

Simply replace a call to `System.loadLibrary` like this:

```java
System.loadLibrary("mylibrary");
```

With a call to `ReLinker.loadLibrary` like this:

```java
ReLinker.loadLibrary(context, "mylibrary");
```

## Advanced Usage

### Asynchronous loading

ReLinker can load libraries asynchronously. Simply pass a `LoadListener` instance to the `loadLibrary` call:
```java
ReLinker.loadLibrary(context, "mylibrary", new ReLinker.LoadListener() {
    @Override
    public void success() { /* Yay */ }

    @Override
    public void failure(Throwable t) { /* Boo */ }
});
```

### Recursive loading

On older versions of Android, the system's library loader may fail to resolve intra-library dependencies. In this instance, ReLinker can resolve those dependencies for you. This will recursively load all libraries defined as "needed" by each library. 

For example, if you have a library `libchild` that relies on `libparent`, then `libchild` will have an entry in its shared object file defining that. ReLinker will parse the shared object file and determine that `libchild` needs `libparent`. ReLinker will then proceed to load `libparent` (and any dependencies it may have) and then `libchild`.  

To allow ReLinker to recursively load and resolve intra-library dependencies simply modify your `loadLibrary` call with the `recursively` modifier, like so:
```java
ReLinker.recursively().loadLibrary(context, "mylibrary");
```

### Logging

To help facilitate debugging, ReLinker can log messages to a `Logger` instance you provide:
```java
ReLinker.log(myLogger).loadLibrary(context, "mylibrary");
```

Which will log the following messages during a normal / successful execution:
```
D/ReLinker: Beginning load of mylibrary...
D/ReLinker: mylibrary was not loaded normally, re-linking...
D/ReLinker: Looking for lib/x86/libmylibrary.so in APK...
D/ReLinker: Found lib/x86/libmylibrary.so! Extracting...
D/ReLinker: mylibrary was re-linked!
```

### Versioning

In the event that your library's code is changed, it is a good idea to specify a specific version. Doing so will allow ReLinker to update the workaround library file successfully. In the case that the system handles the library loading appropriately, the version specified is not used as all library files are extracted and replaced on update or install. 

To specify a version for your library simply provide it as an additional parameter for `loadLibrary` like:
```java
ReLinker.loadLibrary(context, "mylibrary", "1.0");
```

This will cause ReLinker to look for, and load `libmylibrary.so.1.0`. Subsequent version updates will automatically clean up all other library versions.

## Sample application

See the sample application under `sample/` for a quick demo.

## Acknowledgements

Special thanks to [Jeff Young](https://github.com/tenoversix) for the awesome logo!

## License

    Copyright 2015 - 2016 Keepsafe Software Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
