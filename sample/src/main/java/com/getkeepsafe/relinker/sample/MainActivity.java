package com.getkeepsafe.relinker.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.getkeepsafe.relinker.ReLinker;

import java.io.File;

public class MainActivity extends Activity {
    private File mLibDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLibDir = new File(getApplicationInfo().nativeLibraryDir);
        call();
        updateTree();

        findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call();
            }
        });
    }

    private void call() {
        try {
            ((TextView) findViewById(R.id.text)).setText(Native.helloJni());
        } catch (UnsatisfiedLinkError e) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ReLinker.loadLibrary(MainActivity.this, "hellojni");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.text)).setText(Native.helloJni());
                        }
                    });
                }
            }).start();
        }
    }

    private void updateTree() {
        final File[] files = mLibDir.listFiles();
        final StringBuilder builder = new StringBuilder();
        builder.append("Current files in the standard lib directory: ");
        for (final File file : files) {
            builder.append(file.getName()).append(", ");
        }

        ((TextView) findViewById(R.id.tree)).setText(builder.toString());
    }
}
