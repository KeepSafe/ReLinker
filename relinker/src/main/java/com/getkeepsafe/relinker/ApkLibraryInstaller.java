/**
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
import android.content.pm.ApplicationInfo;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkLibraryInstaller implements ReLinker.LibraryInstaller {
    private static final int MAX_TRIES = 5;
    private static final int COPY_BUFFER_SIZE = 4096;

    /**
     * Attempts to unpack the given library to the given destination. Implements retry logic for
     * IO operations to ensure they succeed.
     *
     * @param context {@link Context} to describe the location of the installed APK file
     * @param mappedLibraryName The mapped name of the library file to load
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void installLibrary(final Context context,
                               final String[] abis,
                               final String mappedLibraryName,
                               final File destination,
                               final ReLinkerInstance instance) {
        ZipFile zipFile = null;
        try {
            final ApplicationInfo appInfo = context.getApplicationInfo();
            int tries = 0;
            while (tries++ < MAX_TRIES) {
                try {
                    zipFile = new ZipFile(new File(appInfo.sourceDir), ZipFile.OPEN_READ);
                    break;
                } catch (IOException ignored) {}
            }

            if (zipFile == null) {
                instance.log("FATAL! Couldn't find application APK!");
                return;
            }

            tries = 0;
            while (tries++ < MAX_TRIES) {
                String jniNameInApk = null;
                ZipEntry libraryEntry = null;

                for (final String abi : abis) {
                    jniNameInApk = "lib" + File.separatorChar + abi + File.separatorChar
                            + mappedLibraryName;
                    libraryEntry = zipFile.getEntry(jniNameInApk);

                    if (libraryEntry != null) {
                        break;
                    }
                }

                if (jniNameInApk != null) instance.log("Looking for %s in APK...", jniNameInApk);

                //try to load armeabi lib
                if(libraryEntry == null) {
                    jniNameInApk = "lib" + File.separatorChar + "armeabi" + File.separatorChar + mappedLibraryName;
                    libraryEntry = zipFile.getEntry(jniNameInApk);
                }

                if (libraryEntry == null) {
                    // Does not exist in the APK
                    if (jniNameInApk != null) {
                        throw new MissingLibraryException(jniNameInApk);
                    } else {
                        throw new MissingLibraryException(mappedLibraryName);
                    }
                }

                instance.log("Found %s! Extracting...", jniNameInApk);
                try {
                    if (!destination.exists() && !destination.createNewFile()) {
                        continue;
                    }
                } catch (IOException ignored) {
                    // Try again
                    continue;
                }

                InputStream inputStream = null;
                FileOutputStream fileOut = null;
                try {
                    inputStream = zipFile.getInputStream(libraryEntry);
                    fileOut = new FileOutputStream(destination);
                    final long written = copy(inputStream, fileOut);
                    fileOut.getFD().sync();
                    if (written != destination.length()) {
                        // File was not written entirely... Try again
                        continue;
                    }
                } catch (FileNotFoundException e) {
                    // Try again
                    continue;
                } catch (IOException e) {
                    // Try again
                    continue;
                } finally {
                    closeSilently(inputStream);
                    closeSilently(fileOut);
                }

                // Change permission to rwxr-xr-x
                destination.setReadable(true, false);
                destination.setExecutable(true, false);
                destination.setWritable(true);
                return;
            }

            instance.log("FATAL! Couldn't extract the library from the APK!");
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Copies all data from an {@link InputStream} to an {@link OutputStream}.
     *
     * @param in The stream to read from.
     * @param out The stream to write to.
     * @throws IOException when a stream operation fails.
     * @return The actual number of bytes copied
     */
    private long copy(InputStream in, OutputStream out) throws IOException {
        long copied = 0;
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        while (true) {
            int read = in.read(buf);
            if (read == -1) {
                break;
            }
            out.write(buf, 0, read);
            copied += read;
        }
        out.flush();
        return copied;
    }

    /**
     * Closes a {@link Closeable} silently (without throwing or handling any exceptions)
     * @param closeable {@link Closeable} to close
     */
    private void closeSilently(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {}
    }
}
