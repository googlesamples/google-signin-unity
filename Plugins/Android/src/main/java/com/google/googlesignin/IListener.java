package com.google.googlesignin;

import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public interface IListener extends OnCanceledListener, OnFailureListener
{
    void onAuthenticated(GoogleIdTokenCredential acct);
    void onAuthorized(AuthorizationResult acct);
}