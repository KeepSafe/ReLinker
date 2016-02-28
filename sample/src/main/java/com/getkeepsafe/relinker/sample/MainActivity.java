package com.getkeepsafe.relinker.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.relinker.ReLinker;
import com.getkeepsafe.relinker.elf.Elf;
import com.getkeepsafe.relinker.elf.ElfParser;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private File mLibDir;
    private File mWorkaroundDir;

    private EditText version;

    private ReLinker.Logger logcatLogger = new ReLinker.Logger() {
        @Override
        public void log(String message) {
            Log.d("ReLinker", message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLibDir = new File(getApplicationInfo().nativeLibraryDir);
        mWorkaroundDir = getDir("lib", Context.MODE_PRIVATE);
        updateTree();

        findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call();
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final Process process = Runtime.getRuntime().exec("su");
                    final DataOutputStream stream = new DataOutputStream(process.getOutputStream());
                    stream.writeBytes("rm -r " + mLibDir.getAbsolutePath() + "\n");
                    stream.writeBytes("rm -r " + mWorkaroundDir.getAbsolutePath() + "\n");
                    stream.writeBytes("exit\n");
                    stream.flush();
                    process.waitFor();

                    updateTree();
                    Runtime.getRuntime().exit(0);
                } catch (Throwable e) {
                    Toast.makeText(MainActivity.this, "You do not have root!", Toast.LENGTH_LONG).show();
                }
            }
        });

        version = (EditText) findViewById(R.id.version);
    }

    private void call() {
        try {
            ((TextView) findViewById(R.id.text)).setText(Native.helloJni());
            updateTree();
        } catch (UnsatisfiedLinkError e) {
            final String libVersion = version.getText().toString();
            ReLinker.log(logcatLogger)
                    .force()
                    .recursively()
                    .loadLibrary(MainActivity.this, "hellojni", libVersion,
                            new ReLinker.LoadListener() {
                        @Override
                        public void success() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.text)).setText(Native.helloJni());
                                    updateTree();
                                }
                            });

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final String file;
                                        if (libVersion.length() > 0) {
                                            file = "libhellojni.so." + libVersion;
                                        } else {
                                            file = "libhellojni.so";
                                        }
                                        final File filesDir = getDir("lib", MODE_PRIVATE);
                                        final File lib = new File(filesDir, file);
                                        if (!lib.exists()) return;

                                        final ElfParser parser = new ElfParser(lib);
                                        final List<String> deps = parser.parseNeededDependencies();
                                        final StringBuilder builder = new StringBuilder("Library dependencies:\n");
                                        for (final String str : deps) {
                                            builder.append(str).append(", ");
                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((TextView) findViewById(R.id.deps)).setText(builder.toString());
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }

                        @Override
                        public void failure(Throwable t) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.text)).setText(
                                            "Couldn't load! Report this issue to the github please!");
                                }
                            });
                        }
                    });
        }
    }

    private void updateTree() {
        final File[] files = mLibDir.listFiles();
        final StringBuilder builder = new StringBuilder();
        builder.append("Current files in the standard lib directory: ");
        if (files != null) {
            for (final File file : files) {
                builder.append(file.getName()).append(", ");
            }
        }

        builder.append("\n\nCurrent files in the ReLinker lib directory: ");
        final File[] relinkerFiles = mWorkaroundDir.listFiles();
        if (relinkerFiles != null) {
            for (final File file : relinkerFiles) {
                builder.append(file.getName()).append(", ");
            }
        }

        ((TextView) findViewById(R.id.tree)).setText(builder.toString());
    }
}
