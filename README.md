# Google Sign-In Unity Plugin
_Copyright (c) 2017 Google Inc. All rights reserved._


## Overview

Google Sign-In API plugin for Unity game engine.  Works with Android and iOS.
This plugin exposes the Google Sign-In API within Unity.  This is specifically
intended to be used by Unity projects that require OAuth ID tokens or server
auth codes.

It is cross-platform, supporting both Android and iOS.

See [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
for more information.

## Configuring the application  on the API Console

To authenticate you need to create credentials on the API console for your
application. The steps to do this are available on
[Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
or as part of Firebase configuration.
In order to access ID tokens or server auth codes, you also need to configure
a web client ID.

## How to build the sample


### Get a Google Sign-In configuration file
This file contains the client-side information needed to use Google Sign-in.
The details on how to do this are documented on the [Developer website](https://developers.google.com/identity/sign-in/android/start-integrating#get-config).

Once you have the configuration file, open it in a text editor.  In the middle
of the file you should see the __oauth_client__ section:
```
      "oauth_client": [
        {
          "client_id": "411000067631-hmh4e210xxxxxxxxxx373t3icpju8ooi.apps.googleusercontent.com",
          "client_type": 3
        },
        {
          "client_id": "411000067631-udra361txxxxxxxxxx561o9u9hc0java.apps.googleusercontent.com",
          "client_type": 1,
          "android_info": {
            "package_name": "com.your.package.name.",
            "certificate_hash": "7ada045cccccccccc677a38c91474628d6c55d03"
          }
        }
      ]
```

There are 3 values you need for configuring your Unity project:
1. The __Web client ID__.  This is needed for generating a server auth code for
your backend server, or for generating an ID token.  This is the `client_id`
value for the oauth client with client_type == 3.
2. The __package_name__.  The client entry with client_type == 1 is the
Android client.  The package_name must be entered in the Unity player settings.
3.  The keystore used to sign your application. This is configured in the publishing settings of the Android Player properties in
the Unity editor.  This must be the same keystore used to generate
the SHA1 fingerprint when creating the application on the console.  __NOTE:__
The configutation file does not reference the keystore, you need to keep track of
this yourself.


### Create a new project and import the plugin
Create a new Unity project and import the `GoogleSignIn-1.0.0.unitypackage` (or the latest version).
This contains native code, C# Unity code needed to call the Google Sign-In API for both Android and iOS.

### Import the sample scene
Import the `GoogleSignIn-sample.unitypackage` which contains the sample scene and
scripts.  This package is not needed if you are integrating Google Sign-in into
your own application.

### Configure the web client id
1. Open the sample scene in `Assets/SignInSample/MainScene`.
2. Select the Canvas object in the hierarchy and enter the web client id
in the __SignInSampleScript__ component.

## Building for Android
1. Under Build Settings, select Android as the target platform.
2. Set the package name in the player settings to the package_name you found in
the configuration file.
3. Select the keystore file, the key alias, and passwords.
4. Resolve the Google Play Services SDK dependencies by selecting from the menu:
    __Assets/Play Services Resolver/Android Resolver/Resolve__.  This will add
    the required .aar files to your project in `Assets/Plugins/Android`.

## Building for iOS
For iOS, follow the instructions for creating a GoogleService-Info.plist file on
https://developers.google.com/identity/sign-in/ios/start-integrating.

In Unity, after switching to the iOS player, make sure to run the Play Services
Resolver.  This will add the required frameworks and libraries to the XCode
project via CocoPods.

After generating the XCode project from Unity, download the GoogleService-Info.plist file
from the Google Developers website and add it to your XCode project.

## Using the Games Profile to sign in on Android
To use the Play Games Services Gamer profile when signing in, you need to edit the
dependencies in `Assets/GoogleSignIn/Editor/GoogleSignInDependencies.xml`.

Uncomment the play-services-games dependency and re-run the resolution.


## Using this plugin with Firebase Auth
Follow the instructions to use Firebase Auth with Credentials on the [Firebase developer website]( https://firebase.google.com/docs/unity/setup).

Make sure to copy the google-services.json and/or GoogleService-Info.plist to your Unity project.

Then to use Google SignIn with Firebase Auth, you need to request an ID token when authenticating.
The steps are:
1. Configure Google SignIn to request an id token and set the web client id as described above.
2. Call __SignIn()__ (or __SignInSilently()__).
3. When handling the response, use the ID token to create a Firebase Credential.
4. Call Firebase Auth method  __SignInWithCredential()__.

```
    GoogleSignIn.Configuration = new GoogleSignInConfiguration {
      RequestIdToken = true,
      // Copy this value from the google-service.json file.
      // oauth_client with type == 3
      WebClientId = "1072123000000-iacvb7489h55760s3o2nf1xxxxxxxx.apps.googleusercontent.com"
    };

    Task<GoogleSignInUser> signIn = GoogleSignIn.DefaultInstance.SignIn ();

    TaskCompletionSource<FirebaseUser> signInCompleted = new TaskCompletionSource<FirebaseUser> ();
    signIn.ContinueWith (task => {
      if (task.IsCanceled) {
        signInCompleted.SetCanceled ();
      } else if (task.IsFaulted) {
        signInCompleted.SetException (task.Exception);
      } else {

        Credential credential = Firebase.Auth.GoogleAuthProvider.GetCredential (((Task<GoogleSignInUser>)task).Result.IdToken, null);
        auth.SignInWithCredentialAsync (credential).ContinueWith (authTask => {
          if (authTask.IsCanceled) {
            signInCompleted.SetCanceled();
          } else if (authTask.IsFaulted) {
            signInCompleted.SetException(authTask.Exception);
          } else {
            signInCompleted.SetResult(((Task<FirebaseUser>)authTask).Result);
          }
        });
      }
    });
```


## Building the Plugin
To build the plugin run `./gradlew -PlintAbortOnError build_all`. This builds the support aar
library with lint warnings as errors and packages the plugin into a .unitypackage file.  It
also packages the sample scene and script in a separate package.

There's also a shortcut for linux/mac: `./build_all`.


## Questions? Problems?
Post questions to this [Github project](https://github.com/googlesamples/google-signin-unity).
