# Google Sign-In Unity Plugin
_Copyright (c) 2017 Google Inc. All rights reserved._


## Overview

This plugin exposes the Google Sign-In API within Unity.  This is specifically
intended to be used by Unity projects that require OAuth ID tokens or server
auth codes.

See [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
for more information.

## Configuring the application  on the API Console

To authenticate you need create credentials on the API console for your
application. The steps to do this are available on
[Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
or as part of Firebase configuration.
In order to access ID tokens or server auth codes, you also need to configure
a web client ID.

## Building the Android AAR library
To build the Android library needed by the Unity plugin, run
`gradlew :native-googlesignin:assembleDebug` (or assembleRelease).

Copy the aar file in native-googlesignin/build/outputs/aar to Assets/Plugins/Android.


__TODO:__ How to use this plugin with Firebase Auth.

## Questions? Problems?
Post questions to this [Github project](https://github.com/googlesamples/google-signin-unity).

