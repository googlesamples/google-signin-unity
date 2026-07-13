# Google Sign-In Unity Plugin

Community-maintained Google Sign-In plugin for Unity on Android and iOS. The
public C# API supports Google account profile data, OAuth ID tokens, server auth
codes, additional scopes, and Play Games sign-in.

Version 2.0.0 modernizes the original plugin's build system and platform SDKs
while retaining the existing Unity-facing API.

## Requirements

- Unity 2021.3 LTS or newer. Packaging is verified with Unity 2021.3.14f1.
- Android 6.0 (API level 23) or newer.
- iOS 12.0 or newer.
- External Dependency Manager for Unity (EDM4U), included in the plugin package.

The Android library is built with compile/target SDK 35 and uses Google Play
services Auth 21.6.0, AndroidX Credentials 1.6.0, and Google ID 1.2.0. The iOS
integration uses GoogleSignIn 9.1.0.

## Android Sign-In Behavior

Android 2.0.0 uses Credential Manager for the common Google ID token flow when
all of the following are true:

- `RequestIdToken` and `RequestEmail` are enabled.
- `WebClientId` is set.
- Games sign-in, server auth codes, forced refresh, popup suppression, account
  hints, and additional scopes are not requested.

Requests with advanced options continue through the Google Play services
Google Sign-In compatibility flow. A non-cancellation Credential Manager error
also falls back to that flow. This keeps existing auth-code, Games, and custom
scope configurations working while simple ID-token sign-in uses the modern API.

`SignOut()` clears local sign-in and credential state. `Disconnect()` also
attempts to revoke the account authorization granted to the application.

## Configure OAuth Clients

Create OAuth clients for your application in the
[Google Cloud console](https://console.cloud.google.com/apis/credentials) or
through Firebase:

1. Create a Web application client. Its client ID is the `WebClientId` used for
   ID tokens and server auth codes.
2. Create an Android client with the Unity package name and the SHA-1 fingerprint
   of the keystore that signs the application.
3. For iOS, create an iOS client for the bundle ID and download
   `GoogleService-Info.plist`.

The Android package name and signing certificate must match the Android OAuth
client. Debug and release keystores normally require separate registered SHA-1
fingerprints.

## Import Into Unity

1. Import `google-signin-plugin-2.0.0.unitypackage`.
2. Optionally import `GoogleSignIn-sample.unitypackage` after the plugin. The
   sample package contains only the sample scene and scripts.
3. Open **Assets > External Dependency Manager > Android Resolver > Force
   Resolve** before building Android.

For the sample, open `Assets/SignInSample/MainScene`, select the Canvas, and set
the Web client ID on the `SigninSampleScript` component.

## Build For Android

1. Switch the Unity build target to Android.
2. Set **Player Settings > Other Settings > Minimum API Level** to Android 6.0
   (API level 23) or newer.
3. Set the package name to the value registered for the Android OAuth client.
4. Configure the matching keystore, key alias, and passwords.
5. Run EDM4U **Force Resolve**, then build the application.

To use the Play Games profile, uncomment the `play-services-games` dependency in
`Assets/GoogleSignIn/Editor/GoogleSignInDependencies.xml` and resolve Android
dependencies again.

## Build For iOS

Follow the [Google Sign-In for iOS integration guide](https://developers.google.com/identity/sign-in/ios/start-integrating)
to configure the iOS OAuth client and URL scheme.

1. Set the iOS deployment target to 12.0 or newer.
2. Add `GoogleService-Info.plist` to the Unity project.
3. Switch to the iOS build target and run the EDM4U iOS Resolver.
4. Export the Xcode project and build the generated `.xcworkspace` so the
   CocoaPods dependencies are included.

## Basic Usage

```csharp
GoogleSignIn.Configuration = new GoogleSignInConfiguration {
  WebClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
  RequestEmail = true,
  RequestIdToken = true
};

GoogleSignIn.DefaultInstance.SignIn().ContinueWith(task => {
  if (!task.IsCanceled && !task.IsFaulted) {
    GoogleSignInUser user = task.Result;
    string idToken = user.IdToken;
  }
});
```

For Firebase Authentication, pass the returned ID token to
`GoogleAuthProvider.GetCredential(idToken, null)`, then sign in with the resulting
Firebase credential.

## Build The Plugin

The repository build uses AGP 8.13.2 and Gradle 8.13. Install these prerequisites:

- JDK 17
- Android SDK Platform 35
- Android NDK 29.0.14206865
- CMake 3.22.1 or newer
- Unity, plus Git and network access for fetching EDM4U and Maven dependencies

On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Tools\JDK\open-jdk-17.0.2'
$env:ANDROID_HOME = 'C:\Tools\Android\Sdk'
$env:UNITY_EXE = 'C:\Program Files\Unity\2021.3.14f1\Editor\Unity.exe'
.\gradlew.bat -PlintAbortOnError :native-googlesignin:lintRelease build_all
```

On Linux or macOS, set `JAVA_HOME`, `ANDROID_HOME`, and `UNITY_EXE`, then run:

```bash
./gradlew -PlintAbortOnError :native-googlesignin:lintRelease build_all
```

The build writes these packages to the repository root:

- `google-signin-plugin-2.0.0.unitypackage`
- `GoogleSignIn-sample.unitypackage`

The Android native library is linked for 16 KB page-size compatibility on all
packaged ABIs.

## License

Copyright 2017 Google Inc. and subsequent contributors. Licensed under the
[Apache License 2.0](LICENSE).
