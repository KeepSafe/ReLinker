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
import android.os.Build;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("deprecation")
public class ReLinkerInstance {
    private static final String LIB_DIR = "lib";
    private static final int MAX_TRIES = 5;
    private static final int COPY_BUFFER_SIZE = 4096;

    private ReLinker.Logger logger;
    private boolean force;

    protected ReLinkerInstance() {}

    /**
     * Logs debugging related information to the {@link ReLinker.Logger} instance give
     */
    public ReLinkerInstance log(final ReLinker.Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Forces any previously extracted / re-linked libraries to be cleaned up before loading
     */
    public ReLinkerInstance force() {
        this.force = true;
        return this;
    }

    /**
     * Utilizes the regular system call to attempt to load a native library. If a failure occurs,
     * then the function extracts native .so library out of the app's APK and attempts to load it.
     * <p>
     *     <strong>Note: This is a synchronous operation</strong>
     */
    public void loadLibrary(final Context context, final String library) {
        loadLibrary(context, library, null, null);
    }

    /**
     * The same call as {@link #loadLibrary(Context, String)}, however if a {@code version} is
     * provided, then that specific version of the given library is loaded.
     */
    public void loadLibrary(final Context context, final String library, final String version) {
        loadLibrary(context, library, version, null);
    }

    /**
     * The same call as {@link #loadLibrary(Context, String)}, however if a
     * {@link ReLinker.LoadListener} is provided, the function is executed asynchronously.
     */
    public void loadLibrary(final Context context,
                            final String library,
                            final ReLinker.LoadListener listener) {
        loadLibrary(context, library, null, listener);
    }

    /**
     * Attemps to load the given library normally. If that fails, it loads the library utilizing
     * a workaround.
     *
     * @param context The {@link Context} to get a workaround directory from
     * @param library The library you wish to load
     * @param version The version of the library you wish to load, or {@code null}
     * @param listener {@link ReLinker.LoadListener} to listen for async execution, or {@code null}
     */
    public void loadLibrary(final Context context,
                            final String library,
                            final String version,
                            final ReLinker.LoadListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("Given context is null");
        }

        if (TextUtils.isEmpty(library)) {
            throw new IllegalArgumentException("Given library is either null or empty");
        }

        log("Beginning load of %s...", library);
        if (listener == null) {
            loadLibraryInternal(context, library, version);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadLibraryInternal(context, library, version);
                        listener.success();
                    } catch (UnsatisfiedLinkError e) {
                        listener.failure(e);
                    }
                }
            }).start();
        }
    }

    private void loadLibraryInternal(final Context context,
                                     final String library,
                                     final String version) {
        try {
            System.loadLibrary(library);
            log("%s (%s) was loaded normally!", library, version);
            return;
        } catch (final UnsatisfiedLinkError ignored) {
            // :-(
        }

        log("%s (%s) was not loaded normally, re-linking...", library, version);
        final File workaroundFile = getWorkaroundLibFile(context, library, version);
        if (!workaroundFile.exists() || force) {
            if (force) {
                log("Forcing a re-link of %s (%s)...", library, version);
            }

            cleanupOldLibFiles(context, library, version);
            unpackLibrary(context, library, workaroundFile);
        }

        System.load(workaroundFile.getAbsolutePath());
        log("%s (%s) was re-linked!", library, version);
    }

    /**
     * @param context {@link Context} to describe the location of it's private directories
     * @return A {@link File} locating the directory that can store extracted libraries
     * for later use
     */
    private File getWorkaroundLibDir(final Context context) {
        return context.getDir(LIB_DIR, Context.MODE_PRIVATE);
    }

    /**
     * @param context {@link Context} to retrieve the workaround directory from
     * @param library The name of the library to load
     * @param version The version of the library to load or {@code null}
     * @return A {@link File} locating the workaround library file to load
     */
    private File getWorkaroundLibFile(final Context context,
                                      final String library,
                                      final String version) {
        final String libName = System.mapLibraryName(library);

        if (TextUtils.isEmpty(version)) {
            return new File(getWorkaroundLibDir(context), libName);
        }

        return new File(getWorkaroundLibDir(context), libName + "." + version);
    }

    /**
     * Cleans up any <em>other</em> versions of the {@code library}. If {@code force} is used, all
     * versions of the {@code library} are deleted
     *
     * @param context {@link Context} to retrieve the workaround directory from
     * @param library The name of the library to load
     * @param currentVersion The version of the library to keep, all other versions will be deleted.
     *                       This parameter is ignored if {@code force} is used.
     */
    private void cleanupOldLibFiles(final Context context,
                                    final String library,
                                    final String currentVersion) {
        final File workaroundDir = getWorkaroundLibDir(context);
        final File workaroundFile = getWorkaroundLibFile(context, library, currentVersion);
        final String mappedLibraryName = System.mapLibraryName(library);
        final File[] existingFiles = workaroundDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(mappedLibraryName);
            }
        });

        if (existingFiles == null) return;

        for (final File file : existingFiles) {
            if (force || !file.getAbsolutePath().equals(workaroundFile.getAbsolutePath())) {
                file.delete();
            }
        }
    }

    /**
     * Attempts to unpack the given library to the workaround directory. Implements retry logic for
     * IO operations to ensure they succeed.
     *
     * @param context {@link Context} to describe the location of the installed APK file
     * @param library The name of the library to load
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void unpackLibrary(final Context context,
                               final String library,
                               final File destination) {
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
                return;
            }

            tries = 0;
            while (tries++ < MAX_TRIES) {
                String jniNameInApk = null;
                ZipEntry libraryEntry = null;

                if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS.length > 0) {
                    for (final String ABI : Build.SUPPORTED_ABIS) {
                        jniNameInApk = "lib/" + ABI + "/" + System.mapLibraryName(library);
                        libraryEntry = zipFile.getEntry(jniNameInApk);

                        if (libraryEntry != null) {
                            break;
                        }
                    }
                } else {
                    //noinspection deprecation
                    jniNameInApk = "lib/" + Build.CPU_ABI + "/" + System.mapLibraryName(library);
                    libraryEntry = zipFile.getEntry(jniNameInApk);
                }

                if (jniNameInApk != null) log("Looking for %s in APK...", jniNameInApk);

                if (libraryEntry == null) {
                    // Does not exist in the APK
                    if (jniNameInApk != null) {
                        throw new MissingLibraryException(jniNameInApk);
                    } else {
                        throw new MissingLibraryException(library);
                    }
                }

                log("Found %s! Extracting...", jniNameInApk);
                try {
                    if (!destination.createNewFile()) {
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
                    copy(inputStream, fileOut);
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
                break;
            }
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
     */
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        while (true) {
            int read = in.read(buf);
            if (read == -1) {
                break;
            }
            out.write(buf, 0, read);
        }
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

    private void log(final String format, final Object... args) {
        log(String.format(Locale.US, format, args));
    }

    private void log(final String message) {
        if (logger != null) {
            logger.log(message);
        }
    }
}
