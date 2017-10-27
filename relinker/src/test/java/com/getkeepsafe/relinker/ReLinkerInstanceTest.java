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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class ReLinkerInstanceTest {
    private static final String TEST_LIB = "mylib";
    private static final String TEST_LIB_MAPPED = "libmylib.so";
    private static final String TEST_DIR = "lib";

    @Mock
    Context context;
    @Mock
    ReLinker.LibraryLoader testLoader;
    @Mock
    ReLinker.LibraryInstaller testInstaller;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File libDir;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        tempFolder.create();
        libDir = tempFolder.getRoot();
        when(context.getDir(TEST_DIR, Context.MODE_PRIVATE)).thenReturn(libDir);
        when(testLoader.mapLibraryName(TEST_LIB)).thenReturn(TEST_LIB_MAPPED);
    }

    @Test
    public void getsCorrectWorkaroundDirectory() {
        final ReLinkerInstance instance = new ReLinkerInstance(testLoader, testInstaller);
        assertThat(instance.getWorkaroundLibDir(context), is(libDir));
    }

    @Test
    public void getsCorrectWorkaroundFile() {
        final ReLinkerInstance instance = new ReLinkerInstance(testLoader, testInstaller);
        final String libName = testLoader.mapLibraryName(TEST_LIB);
        final File libFile = new File(libDir, libName);
        assertThat(instance.getWorkaroundLibFile(context, TEST_LIB, null), is(libFile));

        final File versionedLibFile = new File(libDir, libName + ".2.0");
        assertThat(instance.getWorkaroundLibFile(context, TEST_LIB, "2.0"), is(versionedLibFile));
    }

    @Test
    public void cleansupOldLibraryFiles() throws IOException {
        final ReLinkerInstance instance = new ReLinkerInstance(testLoader, testInstaller);
        final String mappedName = testLoader.mapLibraryName(TEST_LIB);
        tempFolder.newFile(mappedName);
        tempFolder.newFile(mappedName + ".2.0");
        tempFolder.newFile(mappedName + ".3.4");
        tempFolder.newFile(mappedName + ".4.0");

        assertThat(libDir.listFiles().length, is(4));
        instance.cleanupOldLibFiles(context, TEST_LIB, "4.0");
        assertThat(libDir.listFiles().length, is(1));

        tempFolder.newFile(mappedName);
        tempFolder.newFile(mappedName + ".2.0");
        tempFolder.newFile(mappedName + ".3.4");
        assertThat(libDir.listFiles().length, is(4));
        instance.cleanupOldLibFiles(context, TEST_LIB, null);
        assertThat(libDir.listFiles().length, is(1));

        tempFolder.newFile(mappedName + ".2.0");
        tempFolder.newFile(mappedName + ".3.4");
        tempFolder.newFile(mappedName + ".4.0");
        assertThat(libDir.listFiles().length, is(4));
        instance.force().cleanupOldLibFiles(context, TEST_LIB, "4.0");
        assertThat(libDir.listFiles().length, is(0));
    }

    @Test
    public void loadsLibraryNormally() {
        final ReLinkerInstance instance = new ReLinkerInstance(testLoader, testInstaller);
        instance.loadLibrary(context, TEST_LIB);
    }

    @Test
    public void relinksLibrary() {
        final ReLinkerInstance instance = new ReLinkerInstance(testLoader, testInstaller);
        final File workaroundFile = new File(libDir.getAbsolutePath(), TEST_LIB_MAPPED);
        final String[] abis = new String[] {"x86"};

        doThrow(new UnsatisfiedLinkError("boo")).when(testLoader).loadLibrary(anyString());
        when(testLoader.supportedAbis()).thenReturn(abis);

        instance.loadLibrary(context, TEST_LIB);
        verify(testLoader).loadLibrary(TEST_LIB);
        verify(testLoader).loadPath(workaroundFile.getAbsolutePath());
        verify(testLoader).supportedAbis();
        verify(testInstaller).installLibrary(context, abis, TEST_LIB_MAPPED, workaroundFile, instance);

        instance.force().loadLibrary(context, TEST_LIB);
        verify(testLoader, times(2)).loadLibrary(TEST_LIB);
        verify(testLoader, times(2)).loadPath(workaroundFile.getAbsolutePath());
        verify(testLoader, times(2)).supportedAbis();
        verify(testInstaller, times(2)).installLibrary(
                context, abis, TEST_LIB_MAPPED, workaroundFile, instance);
    }
}
