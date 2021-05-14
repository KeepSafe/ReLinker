/*
 * Copyright 2015 - 2016 KeepSafe Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getkeepsafe.relinker;

import android.content.Context;

import java.io.File;

/**
 * ReLinker is a small library to help alleviate {@link UnsatisfiedLinkError} exceptions thrown due
 * to Android's inability to properly install / load native libraries for Android versions before
 * API 23.
 */
public class ReLinker {
    public interface LoadListener {
        void success();
        void failure(Throwable t);
    }

    public interface Logger {
        void log(String message);
    }

    public interface LibraryLoader {
        void loadLibrary(String libraryName);
        void loadPath(String libraryPath);
        String mapLibraryName(String libraryName);
        String unmapLibraryName(String mappedLibraryName);
        String[] supportedAbis();
    }

    public interface LibraryInstaller {
        void installLibrary(Context context, String[] abis, String mappedLibraryName,
                            File destination, ReLinkerInstance logger);
    }

    public static void loadLibrary(final Context context, final String library) {
        loadLibrary(context, library, null, null);
    }

    public static void loadLibrary(final Context context,
                                   final String library,
                                   final String version) {
        loadLibrary(context, library, version, null);
    }

    public static void loadLibrary(final Context context,
                                   final String library,
                                   final LoadListener listener) {
        loadLibrary(context, library, null, listener);
    }

    public static void loadLibrary(final Context context,
                            final String library,
                            final String version,
                            final ReLinker.LoadListener listener) {
        new ReLinkerInstance().loadLibrary(context, library, version, listener);
    }

    public static ReLinkerInstance force() {
        return new ReLinkerInstance().force();
    }

    public static ReLinkerInstance log(final Logger logger) {
        return new ReLinkerInstance().log(logger);
    }

    public static ReLinkerInstance recursively() {
        return new ReLinkerInstance().recursively();
    }

    private ReLinker() {}
}
