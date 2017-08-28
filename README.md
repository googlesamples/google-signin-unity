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

## Building the Plugin
To build the plugin run `./gradlew build_all`. This builds the support aar
library, and packages the plugin into a .unitypackage file.  It also packages the
sample scene and script in a separate package.

__TODO:__ How to use this plugin with Firebase Auth.

## Questions? Problems?
Post questions to this [Github project](https://github.com/googlesamples/google-signin-unity).

