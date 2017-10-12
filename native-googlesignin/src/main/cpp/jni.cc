//
// Copyright 2017 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
#include <assert.h>
#include <pthread.h>
#include "jni_init.h"

// static pointer to access the JVM.
static JavaVM *g_vm;

/// Called when the library is loaded by a Java VM.
extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv *env;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }

  g_vm = vm;

  return JNI_VERSION_1_6;
}

namespace googlesignin {

// Static variables used in tracking thread initialization and cleanup.
pthread_key_t jni_env_key;
pthread_once_t pthread_key_initialized = PTHREAD_ONCE_INIT;

void DetachJVMThreads(void *stored_java_vm) {
  assert(stored_java_vm);
  JNIEnv *jni_env;
  JavaVM *java_vm = static_cast<JavaVM *>(stored_java_vm);
  // AttachCurrentThread does nothing if we're already attached, but
  // calling it ensures that the DetachCurrentThread doesn't fail.
  java_vm->AttachCurrentThread(&jni_env, nullptr);
  java_vm->DetachCurrentThread();
}

// Called the first time GetJNIEnv is invoked.
// Ensures that jni_env_key is created and that the destructor is in place.
void SetupJvmDetachOnThreadDestruction() {
  pthread_key_create(&jni_env_key, DetachJVMThreads);
}

// Helper function used to access the jni environment on the current thread.
JNIEnv *GetJniEnv() {
  // Set up the thread key and destructor the first time this is called:
  (void)pthread_once(&pthread_key_initialized,
                     SetupJvmDetachOnThreadDestruction);
  pthread_setspecific(jni_env_key, g_vm);

  JNIEnv *env;
  jint result = g_vm->AttachCurrentThread(&env, nullptr);
  return result == JNI_OK ? env : nullptr;
}

// Find a class, attempting to load the class if it's not found.
jclass FindClass(const char *class_name, jobject activity) {
  JNIEnv *env = GetJniEnv();
  jclass class_object = env->FindClass(class_name);
  if (env->ExceptionCheck()) {
    env->ExceptionClear();
    // If the class isn't found it's possible NativeActivity is being used by
    // the application which means the class path is set to only load system
    // classes.  The following falls back to loading the class using the
    // Activity before retrieving a reference to it.
    jclass activity_class = env->FindClass("android/app/Activity");
    jmethodID activity_get_class_loader = env->GetMethodID(
        activity_class, "getClassLoader", "()Ljava/lang/ClassLoader;");

    jobject class_loader_object =
        env->CallObjectMethod(activity, activity_get_class_loader);

    jclass class_loader_class = env->FindClass("java/lang/ClassLoader");
    jmethodID class_loader_load_class =
        env->GetMethodID(class_loader_class, "loadClass",
                         "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring class_name_object = env->NewStringUTF(class_name);

    class_object = static_cast<jclass>(env->CallObjectMethod(
        class_loader_object, class_loader_load_class, class_name_object));

    if (env->ExceptionCheck()) {
      env->ExceptionClear();
      class_object = nullptr;
    }
    env->DeleteLocalRef(class_name_object);
    env->DeleteLocalRef(class_loader_object);
  }
  return class_object;
}
}  // namespace googlesignin
