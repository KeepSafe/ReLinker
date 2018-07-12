/**
 * Copyright 2015 - 2016 KeepSafe Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getkeepsafe.relinker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class SystemLibraryLoader implements ReLinker.LibraryLoader {

    @Override
    public void loadLibrary(final String libraryName) {
        System.loadLibrary(libraryName);
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
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
        return SDK_INT >= LOLLIPOP ? getSupportedAbis_21() : getSupportedAbisOlder();
    }

    @TargetApi(LOLLIPOP)
    private String[] getSupportedAbis_21() {
        final String[] abis = Build.SUPPORTED_ABIS;
        return abis != null && abis.length > 0 ? abis : getSupportedAbisOlder();
    }

    private String[] getSupportedAbisOlder() {
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }
}