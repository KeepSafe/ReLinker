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

import java.util.Arrays;

public class MissingLibraryException extends RuntimeException {
    public MissingLibraryException(final String library, final String[] wantedABIs, final String[] supportedABIs) {
        super("Could not find '" + library + "'. " +
                "Looked for: " + Arrays.toString(wantedABIs) + ", " +
                "but only found: " + Arrays.toString(supportedABIs) + ".");
    }
}
