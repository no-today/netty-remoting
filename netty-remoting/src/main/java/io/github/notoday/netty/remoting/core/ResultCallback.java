package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.common.ErrorInfo;

/**
 * @author no-today
 * @date 2022/06/27 17:31
 */
public interface ResultCallback<T> {

    void onSuccess(T response);

    void onFailure(ErrorInfo error);
}
