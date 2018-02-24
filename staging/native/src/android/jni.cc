// Copyright 2018 Google Inc. All rights reserved.
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

// This file isolates some of the JNI boilerplate for managing the JNI
// environment, dealing with class loaders and keeping track of threads that
// have been bound to the JavaVM.

#include <assert.h>
#include <pthread.h>
#include <jni.h>
#include "jni_context.h"

namespace google {
namespace signin {

JavaVM* JNIContext::vm_ = nullptr;
static pthread_key_t jni_env_key;
static pthread_once_t pthread_key_initialized = PTHREAD_ONCE_INIT;

// This will be called by any thread that's attached to the JVM when it exits.
extern "C" {
static void DetachJVMThreads(void* stored_java_vm) {
  assert(stored_java_vm);
  JavaVM* java_vm = static_cast<JavaVM *>(stored_java_vm);

  // AttachCurrentThread does nothing if we're already attached, but
  // calling it ensures that the DetachCurrentThread doesn't fail.
  JNIEnv* jni_env;
  java_vm->AttachCurrentThread(&jni_env, nullptr);
  java_vm->DetachCurrentThread();
}

// Called the first time GetJNIEnv is invoked.
// Ensures that jni_env_key is created and that the destructor is in place.
static void SetupJvmThreadStorage() {
  // Set up the thread destructor when the JavaVM is first known.
  // This creates a key for a thread local value. The destructor is then
  // called for any thread (when it exits), which has assigned a value
  // to this key, using setspecific.
  pthread_key_create(&jni_env_key, DetachJVMThreads);
}
}

JNIContext::JNIContext(jobject activity, JavaVM *vm) {
  vm_ = vm;
  (void)pthread_once(&pthread_key_initialized,
                     SetupJvmThreadStorage);

  JNIEnv* env = GetJniEnv();
  activity_ = env->NewGlobalRef(activity);
}

JNIContext::~JNIContext() {
  JNIEnv* env = GetJniEnv();
  env->DeleteGlobalRef(activity_);
  activity_ = nullptr;

  // we don't clear the vm_ in case there are multiple contexts.
}

// This could be static, but it should never be called before at least one
// instance is constructed.
JNIEnv* JNIContext::GetJniEnv() {
  assert(vm_);
  // This call allows us to set a thread-local value. Even though there will
  // only ever be one JavaVM, and every thread will store the same value,
  // this ensures any thread attaching to the JVM will call the thread
  // destructor to detach before exiting.
  pthread_setspecific(jni_env_key, vm_);
  JNIEnv* env;
  jint result = vm_->AttachCurrentThread(&env, nullptr);
  return result == JNI_OK ? env : nullptr;
}

// Returns a local reference to the activity used to construct this class.
// The user is expected to call DeleteLocalRef to avoid leaking.
jobject JNIContext::GetActivity() {
  assert(activity_);
  JNIEnv* env = GetJniEnv();
  return env->NewLocalRef(activity_);
}

// Find a class, attempting to load the class if it's not found.
jclass JNIContext::FindClass(const char *class_name) {
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

    jobject activity_instance = GetActivity();
    jobject class_loader_object =
        env->CallObjectMethod(activity_instance, activity_get_class_loader);

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
    env->DeleteLocalRef(activity_instance);
    env->DeleteLocalRef(class_name_object);
    env->DeleteLocalRef(class_loader_object);
  }
  return class_object;
}

std::string JNIContext::JStringAsString(jstring j_str) {
  if (!j_str) {
    return "";
  }
  JNIEnv* env = GetJniEnv();
  const char* buf = env->GetStringUTFChars(j_str, nullptr);
  std::string return_string(buf);
  env->ReleaseStringUTFChars(j_str, buf);
  return return_string;
}

std::string JNIContext::JStringToString(jstring j_str) {
  JNIEnv* env = GetJniEnv();
  std::string return_string = JStringAsString(j_str);
  env->DeleteLocalRef(j_str);
  return return_string;
}

}  // namespace signin
}  // namespace google
