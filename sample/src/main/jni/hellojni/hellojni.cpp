#include <jni.h>

extern "C"
{
    JNIEXPORT jstring
    JNICALL Java_com_getkeepsafe_relinker_sample_Native_helloJni(JNIEnv *env, jclass clazz) {
        return env->NewStringUTF("Hello from the JNI!");
    }
}