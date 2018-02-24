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

#ifndef GOOGLESIGNIN_JNI_INIT_H
#define GOOGLESIGNIN_JNI_INIT_H

// All methods in this header are meant for Android only.

#include <pthread.h>
#include <jni.h>
#include <string>

namespace google {
namespace signin {

// This stores the JavaVM, assuming there's only ever one, and a global
// reference to an Activity. You can instantiate this for each activity, though
// any activity will work for all cases as it's just used to get access to
// non-system class loaders when using the FindClass method.
// Any thread that calls GetJniEnv is bound to the JavaVM and will
// automatically be detached when that thread exits.
class JNIContext {
public:
  JNIContext(jobject activity, JavaVM *vm);
  ~JNIContext();

  // This could be static, but it should never be called before at least one
  // instance is constructed.
  JNIEnv* GetJniEnv();

  // Returns a local reference to the activity used to construct this class.
  // The user is expected to call DeleteLocalRef to avoid leaking.
  jobject GetActivity();

  // Find a class, attempting to load the class if it's not found.
  jclass FindClass(const char *class_name);

  // Copies a Java String to an std::string. This does not delete the local
  // reference to the jstring.
  std::string JStringAsString(jstring j_str);

  // Like JStringAsString, but will also delete the local reference to the
  // java string.
  std::string JStringToString(jstring j_str);

private:
  // The docs say this has to be a c function.
  // not sure if that means we cannot have it as a static.
  // But, if there's a problem move this outside the class and use:
  // extern "C" void DetachJVMThreads(void* thread_set_jvm);
  static void DetachJVMThreads(void* stored_java_vm);

  static JavaVM *vm_;
  jobject activity_;
};

}  // namespace signin
}  // namespace google

#endif  // GOOGLESIGNIN_JNI_INIT_H