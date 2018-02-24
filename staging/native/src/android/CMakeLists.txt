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

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

include_directories( ../include )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             google-signin-cpp

             # Sets the library as a shared library.
             STATIC

             # Provides a relative path to your source file(s).
             google_signin.cc
             google_signin_user.cc
             jni.cc)

set(PACKAGE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../google-signin-cpp)
MAKE_DIRECTORY("${PACKAGE_DIR}/include")
set_target_properties(google-signin-cpp
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      "${PACKAGE_DIR}/lib/${ANDROID_ABI}")

add_custom_command(TARGET google-signin-cpp POST_BUILD
                   COMMAND "${CMAKE_COMMAND}" -E
                   copy "${CMAKE_CURRENT_SOURCE_DIR}/../include/*.h"
                   "${PACKAGE_DIR}/include"
                   COMMENT "Copying public headers to output package")