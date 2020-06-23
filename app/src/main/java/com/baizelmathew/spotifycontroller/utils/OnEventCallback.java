/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.utils;

/**
 * helper class for play back event change callbacks
 */
public interface OnEventCallback<T> {
    void onResult(T result);

    void onFailure(Throwable ex);
}
