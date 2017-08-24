
package com.ponysdk.core.server.concurrent;

import com.ponysdk.core.server.application.UIContext;

import java.util.function.Consumer;

public final class UIDelegator<R> implements Consumer<R> {

    private final Consumer<R> consumer;
    private final UIContext uiContext;

    UIDelegator(final Consumer<R> callback, final UIContext uiContext) {
        this.uiContext = uiContext;
        this.consumer = callback;
    }

    @Override
    public void accept(final R t) {
        PScheduler.schedule(uiContext, () -> consumer.accept(t));
    }

}