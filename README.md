# ReLinker

## Overview

Unfortunately Android's PackageManager seems to be flaky with its ability to properly install native libraries from your APK on older Android versions. ReLinker is here to alleviate that problem by replacing the standard `System.loadLibrary` call

## Installation

Add the following dependency to your gradle build file:

    dependencies {
        compile 'com.getkeepsafe.relinker:library:1.0'
    }

## Usage

Instead of calling `System.loadLibrary`, simply call `ReLinker.loadLibrary`. Note that ReLinker require's a `Context`, which can and should be your Application's context.

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