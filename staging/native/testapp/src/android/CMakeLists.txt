# Copyright 2018 Google Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)

add_library(native-app-glue
    STATIC
    ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c)

# now build app's shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -Wall -Werror")

set(GSI_PACKAGE_DIR "${CMAKE_SOURCE_DIR}/../../../google-signin-cpp")
add_library(lib-google-signin-cpp STATIC IMPORTED)
set_target_properties(lib-google-signin-cpp PROPERTIES IMPORTED_LOCATION
    ${GSI_PACKAGE_DIR}/lib/${ANDROID_ABI}/libgoogle-signin-cpp.a )

# Our testapp is an SO the native app loads.
add_library(signin-testapp
    SHARED
    ../common_main.cpp
    android_main.cpp)

# Export ANativeActivity_onCreate(),
# Refer to: https://github.com/android-ndk/ndk/issues/381.
set(CMAKE_SHARED_LINKER_FLAGS
    "${CMAKE_SHARED_LINKER_FLAGS} -u ANativeActivity_onCreate")

# Add lib inlcudes
target_include_directories(signin-testapp
    PRIVATE
    ${ANDROID_NDK}/sources/android/native_app_glue
    ${GSI_PACKAGE_DIR}/include
    ..)

# add lib dependencies
target_link_libraries(signin-testapp
    android
    lib-google-signin-cpp
    native-app-glue
    log)
