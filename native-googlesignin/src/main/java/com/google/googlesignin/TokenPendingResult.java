/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.googlesignin;

import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pending result class for TokenResult. This allows the pending result to be returned to the
 * caller, and then updated when available, simplifying the handling of callbacks.
 */
public class TokenPendingResult extends PendingResult<TokenResult> {

  private static final String TAG = "TokenPendingResult";
  private final long requestHandle;

  private CountDownLatch latch = new CountDownLatch(1);
  private TokenResult result;
  private ResultCallback<? super TokenResult> resultCallback;

  public TokenPendingResult(long requestHandle) {
    this.requestHandle = requestHandle;
    result = new TokenResult();
    result.setHandle(requestHandle);
  }

  @Override
  public TokenResult await() {

    try {
      latch.await();
    } catch (InterruptedException e) {
      setResult(null, CommonStatusCodes.INTERRUPTED);
    }

    return getResult();
  }

  @Override
  public TokenResult await(long l, TimeUnit timeUnit) {
    try {
      if (!latch.await(l, timeUnit)) {
        setResult(null, CommonStatusCodes.TIMEOUT);
      }
    } catch (InterruptedException e) {
      setResult(null, CommonStatusCodes.INTERRUPTED);
    }
    return getResult();
  }

  @Override
  public void cancel() {
    setResult(null, CommonStatusCodes.CANCELED);
    latch.countDown();
  }

  @Override
  public boolean isCanceled() {
    return getResult() != null && getResult().getStatus().isCanceled();
  }

  @Override
  public void setResultCallback(ResultCallback<? super TokenResult> resultCallback) {

    // Handle adding the callback when the latch has already counted down.  This
    // can happen if there is an error right away.
    if (latch.getCount() == 0) {
      resultCallback.onResult(getResult());
    } else {
      setCallback(resultCallback);
    }
  }

  @Override
  public void setResultCallback(
      ResultCallback<? super TokenResult> resultCallback, long l, TimeUnit timeUnit) {
    try {
      if (!latch.await(l, timeUnit)) {
        setResult(null, CommonStatusCodes.TIMEOUT);
      }
    } catch (InterruptedException e) {
      setResult(null, CommonStatusCodes.INTERRUPTED);
    }

    resultCallback.onResult(getResult());
  }

  private synchronized void setCallback(ResultCallback<? super TokenResult> callback) {
    this.resultCallback = callback;
  }

  private synchronized ResultCallback<? super TokenResult> getCallback() {
    return this.resultCallback;
  }

  /**
   * Set the result. If any of the values are null, and a previous non-null value was set, the
   * non-null value is retained.
   *
   * @param account - the signin account, if any.
   * @param resultCode - the result code.
   */
  public synchronized void setResult(GoogleSignInAccount account, int resultCode) {
    result = new TokenResult(account, resultCode);
    result.setHandle(requestHandle);
  }

  private synchronized TokenResult getResult() {
    return result;
  }

  /**
   * Sets the result status and releases the latch and/or calls the callback.
   *
   * @param status - the result status.
   */
  public void setStatus(int status) {
    result.setStatus(status);
    latch.countDown();
    ResultCallback<? super TokenResult> cb = getCallback();
    TokenResult res = getResult();
    if (cb != null) {
      Log.d(TAG, " Calling onResult for callback. result: " + res);
      getCallback().onResult(res);
    }
  }
}
