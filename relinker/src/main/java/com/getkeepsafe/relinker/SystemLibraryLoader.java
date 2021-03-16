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

import android.annotation.SuppressLint;
import android.os.Build;

@SuppressWarnings("deprecation")
final class SystemLibraryLoader implements ReLinker.LibraryLoader {
    @Override
    public void loadLibrary(final String libraryName) {
        System.loadLibrary(libraryName);
    }

    @SuppressLint ("UnsafeDynamicallyLoadedCode")
    @Override
    public void loadPath(final String libraryPath) {
        System.load(libraryPath);
    }

    @Override
    public String mapLibraryName(final String libraryName) {
        if (libraryName.startsWith("lib") && libraryName.endsWith(".so")) {
            // Already mapped
            return libraryName;
        }

        return System.mapLibraryName(libraryName);
    }

    @Override
    public String unmapLibraryName(String mappedLibraryName) {
        // Assuming libname.so
        return mappedLibraryName.substring(3, mappedLibraryName.length() - 3);
    }

    @Override
    public String[] supportedAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS;
        } else if (!TextUtils.isEmpty(Build.CPU_ABI2)) {
            return new String[] {Build.CPU_ABI, Build.CPU_ABI2};
        } else {
            return new String[] {Build.CPU_ABI};
        }
    }
}
