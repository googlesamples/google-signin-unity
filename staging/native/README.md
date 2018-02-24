# Google Sign-In C++ SDK
_Copyright (c) 2018 Google Inc. All rights reserved._


## Overview

This is the Google Sign-In C++ API, for Android and iOS. The intent is to unify
the iOS (Objective-C) and Android (Java) native interfaces into a single common
C++ interface that abstracts all of the platform specific details, however
currently only the Android implementation is supported.

This is intended for projects that require OAuth ID tokens for server auth
codes.

### Getting Started

See
[Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
for information on getting started.

### Dependencies

On Android you'll need to add the dependency
`com.google.android.gms:play-services-auth`, based on the
[setup guide](https://developers.google.com/identity/sign-in/android/start-integrating).


## Configuring the application on the API Console

To authenticate you need to create credentials on the
API console for your application. The steps to do this are available on
[Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start).
In order to access ID tokens or server auth codes, you also need to configure a
*web* client ID. From the
[Credentials Page on the API Console](https://console.developers.google.com/apis/credentials)
select the "Create credentials" dropdown, and choose OAuth Client ID. On the
next page choose Web Application, name the client whatever you like, and then
click "Create".


## How to build the sample

First be sure to edit the sample source configuration:

In testapp/src/common_main.cpp, replace "YOUR_WEB_CLIENT_ID_HERE" with your web
client id, from the configuration step above.

### Building for Android

The android build is setup using gradle.

Navigate to the testapp directory, and build using the "build" target:

cd testapp
./gradlew build
