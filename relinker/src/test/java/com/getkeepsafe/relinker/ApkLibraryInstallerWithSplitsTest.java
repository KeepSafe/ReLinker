package com.getkeepsafe.relinker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
public class ApkLibraryInstallerWithSplitsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock Context context;

    @Mock ApplicationInfo applicationInfo;

    @Mock ReLinkerInstance instance;

    private final String[] abis = new String[]{"x86"};

    private ApkLibraryInstaller subject;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        tempFolder.create();
        subject = new ApkLibraryInstaller();
    }

    @Test
    public void nullSplitSourceDirInstallCorrectly() throws IOException {
        final File destination = tempFolder.newFile("null-test");

        applicationInfo.sourceDir = getClass().getResource("/fake.apk").getFile();
        applicationInfo.splitSourceDirs = null;
        when(context.getApplicationInfo()).thenReturn(applicationInfo);

        subject.installLibrary(context, abis, "libtest.so", destination, instance);
        verify(context).getApplicationInfo();
        assertThat(fileToString(destination), is("works!"));
    }

    @Test
    public void emptySplitSourceDirInstallCorrectly() throws IOException {
        final File destination = tempFolder.newFile("empty-test");

        applicationInfo.sourceDir = getClass().getResource("/fake.apk").getFile();
        applicationInfo.splitSourceDirs = new String[]{};
        when(context.getApplicationInfo()).thenReturn(applicationInfo);

        subject.installLibrary(context, abis, "libtest.so", destination, instance);
        verify(context).getApplicationInfo();
        assertThat(fileToString(destination), is("works!"));
    }

    @Test
    public void apkSplitsInstallCorrectly() throws IOException {
        final File destination = tempFolder.newFile("split-test");

        applicationInfo.sourceDir = "/fake/path/nolib.apk";
        String actualApk = getClass().getResource("/fake.apk").getFile();
        applicationInfo.splitSourceDirs = new String[]{"/another/fake/path/nolib.apk", actualApk};
        when(context.getApplicationInfo()).thenReturn(applicationInfo);

        subject.installLibrary(context, abis, "libtest.so", destination, instance);
        verify(context).getApplicationInfo();
        assertThat(fileToString(destination), is("works!"));
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
