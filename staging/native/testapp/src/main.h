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

#ifndef TESTAPP_MAIN_H
#define TESTAPP_MAIN_H

#if defined(__ANDROID__)
#include <android/native_activity.h>
#include <jni.h>
jobject GetActivity();
JavaVM* GetJavaVM();
#elif defined(__APPLE__)
extern "C" {
#include <objc/objc.h>
}  // extern "C"
#endif  // __ANDROID__

// Fallback, if not defined using -DAPP_NAME=some_app_name when compiling
// this file.
#ifndef APP_NAME
#define APP_NAME "gsi_testapp"
#endif  // APP_NAME

// WindowContext represents the handle to the parent window.  It's type
// (and usage) vary based on the OS.
#if defined(__ANDROID__)
typedef jobject WindowContext;  // A jobject to the Java Activity.
#elif defined(__APPLE__)
typedef id WindowContext;  // A pointer to an iOS UIView.
#else
typedef void* WindowContext;  // A void* for any other environments.
#endif

// Cross platform logging method.
// Implemented by android/android_main.cc or ios/ios_main.mm.
extern "C" void LogMessage(const char* format, ...);

// Cross platform method to flush pending events for the main thread.
// Implemented by android/android_main.cc or ios/ios_main.mm.
// Returns true when an event requesting program-exit is received.
bool ProcessEvents(int msec);

#endif  // TESTAPP_MAIN_H