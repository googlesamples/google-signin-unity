/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.googlesignin;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import java.util.Locale;

/** Class for returning the tokens to a native caller. */
public class TokenResult implements Result {
  private Status status;
  private GoogleSignInAccount account;
  private long handle;

  TokenResult() {
    status = new Status(CommonStatusCodes.SIGN_IN_REQUIRED);
    account = null;
  }

  TokenResult(GoogleSignInAccount account, int resultCode) {
    status = new Status(resultCode);
    this.account = account;
  }

  @Override
  public String toString() {
    return String.format(
        Locale.getDefault(), "Status: %s %s", status, (account == null) ? "<null>" : account);
  }

  @Override
  public Status getStatus() {
    return status;
  }

  public GoogleSignInAccount getAccount() {
    return account;
  }

  public void setStatus(int status) {
    this.status = new Status(status);
  }

  public long getHandle() {
    return handle;
  }

  public void setHandle(long handle) {
    this.handle = handle;
  }
}
