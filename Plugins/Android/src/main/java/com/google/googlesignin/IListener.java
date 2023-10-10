package com.google.googlesignin;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public interface IListener
{
  void OnResult(int result, GoogleSignInAccount acct);
}