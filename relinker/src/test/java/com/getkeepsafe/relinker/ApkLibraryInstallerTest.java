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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApkLibraryInstallerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        tempFolder.create();
    }

    @Test
    public void installsCorrectly() throws IOException {
        final Context context = mock(Context.class);
        final ApplicationInfo appInfo = mock(ApplicationInfo.class);
        final ReLinkerInstance instance = mock(ReLinkerInstance.class);
        final ApkLibraryInstaller installer = new ApkLibraryInstaller();
        final File destination = tempFolder.newFile("test");
        final String[] abis = new String[] {"x86"};

        when(context.getApplicationInfo()).thenReturn(appInfo);
        appInfo.sourceDir = getClass().getResource("/fake.apk").getFile();

        installer.installLibrary(context, abis, "libtest.so", destination, instance);
        verify(context).getApplicationInfo();
        assertThat(fileToString(destination), is("works!"));
    }

    @Test
    public void throwsMissingLibraryExceptionWhenABIIsMissing() throws IOException {
        final Context context = mock(Context.class);
        final ApplicationInfo appInfo = mock(ApplicationInfo.class);
        final ReLinkerInstance instance = mock(ReLinkerInstance.class);
        final ApkLibraryInstaller installer = new ApkLibraryInstaller();
        final File destination = tempFolder.newFile("test");
        final String[] abis = new String[] {"armeabi-v7a"}; // For unit test running on a developer machine this is normally x86

        when(context.getApplicationInfo()).thenReturn(appInfo);
        appInfo.sourceDir = getClass().getResource("/fake.apk").getFile();

        try {
            installer.installLibrary(context, abis, "libtest.so", destination, instance);
        } catch (MissingLibraryException e) {
            assertEquals("Could not find 'libtest.so'. Looked for: [armeabi-v7a], but only found: [x86].", e.getMessage());
        }
    }

    private String fileToString(final File file) throws IOException {
        final long size = file.length();
        if (size > Integer.MAX_VALUE) {
            throw new IOException("Can't read a file larger than Integer.MAX_VALUE");
        }

        final byte[] data = new byte[(int) size];
        final FileInputStream in = new FileInputStream(file);
        try {
            int bytesRead = 0;
            while (bytesRead < size) {
                int read = in.read(data, bytesRead, (int) size - bytesRead);
                if (read == -1) {
                    throw new EOFException();
                }
                bytesRead += read;
            }

            return new String(data);
        } finally {
            in.close();
        }
    }
}
