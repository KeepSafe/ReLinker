package com.getkeepsafe.relinker.sample;

import com.getkeepsafe.relinker.ReLinker;

public class Native {
    static {
        ReLinker.loadLibrary(App.sContext, "hellojni");
    }

    public static native String helloJni();
}
