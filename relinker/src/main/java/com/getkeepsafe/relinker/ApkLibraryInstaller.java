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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkLibraryInstaller implements ReLinker.LibraryInstaller {
    private static final int MAX_TRIES = 5;
    private static final int COPY_BUFFER_SIZE = 4096;

    private String[] sourceDirectories(final Context context) {
        final ApplicationInfo appInfo = context.getApplicationInfo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            appInfo.splitSourceDirs != null &&
            appInfo.splitSourceDirs.length != 0) {
            String[] apks = new String[appInfo.splitSourceDirs.length + 1];
            apks[0] = appInfo.sourceDir;
            System.arraycopy(appInfo.splitSourceDirs, 0, apks, 1, appInfo.splitSourceDirs.length);
            return apks;
        } else {
            return new String[] { appInfo.sourceDir };
        }
    }

    private static class ZipFileInZipEntry {
        public ZipFile zipFile;
        public ZipEntry zipEntry;

        public ZipFileInZipEntry(ZipFile zipFile, ZipEntry zipEntry) {
            this.zipFile = zipFile;
            this.zipEntry = zipEntry;
        }
    }

    private ZipFileInZipEntry findAPKWithLibrary(final Context context,
                                                 final String[] abis,
                                                 final String mappedLibraryName,
                                                 final ReLinkerInstance instance) {

        for (String sourceDir : sourceDirectories(context)) {
            ZipFile zipFile = null;
            int tries = 0;
            while (tries++ < MAX_TRIES) {
                try {
                    zipFile = new ZipFile(new File(sourceDir), ZipFile.OPEN_READ);
                    break;
                } catch (IOException ignored) {
                }
            }

            if (zipFile == null) {
                continue;
            }

            tries = 0;
            while (tries++ < MAX_TRIES) {
                String jniNameInApk = null;
                ZipEntry libraryEntry = null;

                for (final String abi : abis) {
                    jniNameInApk = "lib" + File.separatorChar + abi + File.separatorChar
                            + mappedLibraryName;

                    instance.log("Looking for %s in APK %s...", jniNameInApk, sourceDir);

                    libraryEntry = zipFile.getEntry(jniNameInApk);

                    if (libraryEntry != null) {
                        return new ZipFileInZipEntry(zipFile, libraryEntry);
                    }
                }
            }

            try {
                zipFile.close();
            } catch (IOException ignored) {
            }
        }

        return null;
    }

    // Loop over all APK's again in order to detect which ABI's are actually supported.
    // This second loop is more expensive than trying to find a specific ABI, so it should
    // only be ran when no matching libraries are found. This should keep the overhead of
    // the happy path to a minimum.
    private String[] getSupportedABIs(Context context, String mappedLibraryName) {
        String p = "lib" + File.separatorChar + "([^\\" + File.separatorChar + "]*)" + File.separatorChar + mappedLibraryName;
        Pattern pattern = Pattern.compile(p);
        ZipFile zipFile;
        Set<String> supportedABIs = new HashSet<String>();
        for (String sourceDir : sourceDirectories(context)) {
            try {
                zipFile = new ZipFile(new File(sourceDir), ZipFile.OPEN_READ);
            } catch (IOException ignored) {
                continue;
            }

            Enumeration<? extends ZipEntry> elements = zipFile.entries();
            while (elements.hasMoreElements()) {
                ZipEntry el = elements.nextElement();
                Matcher match = pattern.matcher(el.getName());
                if (match.matches()) {
                    supportedABIs.add(match.group(1));
                }
            }
        }

        String[] result = new String[supportedABIs.size()];
        return supportedABIs.toArray(result);
    }

    /**
     * Attempts to unpack the given library to the given destination. Implements retry logic for
     * IO operations to ensure they succeed.
     *
     * @param context {@link Context} to describe the location of the installed APK file
     * @param mappedLibraryName The mapped name of the library file to load
     */
    @SuppressLint ("SetWorldReadable")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void installLibrary(final Context context,
                               final String[] abis,
                               final String mappedLibraryName,
                               final File destination,
                               final ReLinkerInstance instance) {
        ZipFileInZipEntry found = null;
        try {
            found = findAPKWithLibrary(context, abis, mappedLibraryName, instance);
            if (found == null) {
                // Does not exist in any APK. Report exactly what ReLinker is looking for and
                // what is actually supported by the APK.
                String[] supportedABIs;
                try {
                    supportedABIs = getSupportedABIs(context, mappedLibraryName);
                } catch (Exception e) {
                    // Should never happen as this indicates a bug in ReLinker code, but just to be safe.
                    // User code should only ever crash with a MissingLibraryException if getting this far.
                    supportedABIs = new String[1];
                    supportedABIs[0] = e.toString();
                }
                throw new MissingLibraryException(mappedLibraryName, abis, supportedABIs);
            }

            int tries = 0;
            while (tries++ < MAX_TRIES) {
                instance.log("Found %s! Extracting...", mappedLibraryName);
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
                    inputStream = found.zipFile.getInputStream(found.zipEntry);
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
                if (found != null && found.zipFile != null) {
                    found.zipFile.close();
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
