/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.eventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.eventbus.Event.Type;

public abstract class AbstractEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventBus.class);

    private final Set<BroadcastEventHandler> broadcastHandlerManager = new HashSet<>();

    private final Map<Event.Type, Map<Object, Set<EventHandler>>> map = new HashMap<>();
    private final Queue<Event<? extends EventHandler>> eventQueue = new LinkedList<>();
    private final List<HandlerContext> pendingHandlerRegistration = new ArrayList<>();
    private boolean firing = false;

    @Override
    public HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        else if (handler == null) throw new NullPointerException("Cannot add a null handler");
        else return doAdd(type, null, handler);
    }

    @Override
    public HandlerRegistration addHandlerToSource(final Event.Type type, final Object source, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        else if (source == null) throw new NullPointerException("Cannot add a handler with a null source");
        else if (handler == null) throw new NullPointerException("Cannot add a null handler");
        else return doAdd(type, source, handler);
    }

    private HandlerRegistration doAdd(final Event.Type type, final Object source, final EventHandler handler) {
        if (!firing) doAddNow(type, source, handler);
        else defferedAdd(type, source, handler);

        return () -> doRemove(type, source, handler);
    }

    private void doAddNow(final Event.Type type, final Object source, final EventHandler handler) {
        Map<Object, Set<EventHandler>> sourceMap = map.get(type);
        if (sourceMap == null) {
            sourceMap = new HashMap<>();
            map.put(type, sourceMap);
        }

        // safe, we control the puts.
        Set<EventHandler> handlers = sourceMap.get(source);
        if (handlers == null) {
            handlers = createHandlerSet();
            sourceMap.put(source, handlers);
        }

        handlers.add(handler);
    }

    protected abstract Set<EventHandler> createHandlerSet();

    private void defferedAdd(final Event.Type type, final Object source, final EventHandler handler) {
        final HandlerContext context = new HandlerContext(type, source, handler, true);
        pendingHandlerRegistration.add(context);
    }

    @Override
    public void removeHandler(final Event.Type type, final EventHandler handler) {
        doRemove(type, null, handler);
    }

    @Override
    public void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        doRemove(type, source, handler);
    }

    private void doRemove(final Event.Type type, final Object source, final EventHandler handler) {
        if (!firing) doRemoveNow(type, source, handler);
        else defferedRemove(type, source, handler);
    }

    private void doRemoveNow(final Event.Type type, final Object source, final EventHandler handler) {
        final Map<Object, Set<EventHandler>> sourceMap = map.get(type);
        if (sourceMap == null) return;

        final Set<EventHandler> handlers = sourceMap.get(source);
        if (handlers == null) return;

        final boolean removed = handlers.remove(handler);
        if (!removed) log.warn("Useless remove call : {}", handler);

        if (removed && handlers.isEmpty()) {
            final Set<EventHandler> pruned = sourceMap.remove(source);

            if (pruned != null) {
                if (!pruned.isEmpty() && log.isInfoEnabled()) log.info("Pruned unempty list! {}", pruned);
                if (sourceMap.isEmpty()) map.remove(type);
            } else {
                if (log.isInfoEnabled()) log.info("Can't prune what wasn't there {}", source);
            }
        }
    }

    private void defferedRemove(final Event.Type type, final Object source, final EventHandler handler) {
        final HandlerContext context = new HandlerContext(type, source, handler, false);
        final boolean removed = pendingHandlerRegistration.remove(context);
        if (!removed) pendingHandlerRegistration.add(context);
    }

    @Override
    public void fireEvent(final Event<? extends EventHandler> event) {
        if (event == null) throw new NullPointerException("Cannot fire null eventbus");
        else doFire(event, null);
    }

    @Override
    public void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        if (event == null) throw new NullPointerException("Cannot fire null eventbus");
        else if (source == null) throw new NullPointerException("Cannot fire from a null source");
        else doFire(event, source);
    }

    private void doFire(final Event<? extends EventHandler> event, final Object source) {
        if (source != null) event.setSource(source);

        eventQueue.add(event);

        if (firing) return;

        firing = true;

        try {
            Event e;
            Set<Throwable> causes = null;

            while ((e = eventQueue.poll()) != null) {
                final Object eventSource = e.getSource();
                final Type eventType = e.getAssociatedType();
                final Collection<? extends EventHandler> handlers;
                final Collection<EventHandler> directHandlers = getHandlers(eventType, eventSource);
                if (eventSource != null) {
                    final Collection<EventHandler> globalHandlers = getHandlers(eventType, null);
                    final Set<EventHandler> rtn = new LinkedHashSet<>(directHandlers);
                    rtn.addAll(globalHandlers);
                    handlers = rtn;
                } else {
                    handlers = directHandlers;
                }

                for (final EventHandler handler1 : handlers) {
                    try {
                        if (log.isDebugEnabled()) log.debug("dispatch eventbus #" + e);
                        e.dispatch(handler1);
                    } catch (final Throwable t) {
                        log.error("Cannot process fired eventbus #" + eventType, t);
                        if (causes == null) {
                            causes = new HashSet<>();
                        }
                        causes.add(t);
                    }
                }

                for (final BroadcastEventHandler handler : broadcastHandlerManager) {
                    if (log.isDebugEnabled()) log.debug("broadcast eventbus #" + e);
                    handler.onEvent(e);
                }
            }

            for (final HandlerContext context : pendingHandlerRegistration) {
                if (context.add) doAddNow(context.type, context.source, context.handler);
                else doRemoveNow(context.type, context.source, context.handler);
            }

            pendingHandlerRegistration.clear();

            if (causes != null) throw new UmbrellaException(causes);
        } finally {
            firing = false;
        }
    }

    @Override
    public Collection<EventHandler> getHandlers(final Event.Type type, final Object source) {
        final Map<Object, Set<EventHandler>> sourceMap = map.get(type);
        if (sourceMap == null) return Collections.emptySet();

        // safe, we control the puts.
        final Set<EventHandler> handlers = sourceMap.get(source);
        if (handlers != null) return new HashSet<>(handlers);
        else return Collections.emptySet();
    }

    @Override
    public void addHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.add(handler);
    }

    @Override
    public void removeHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.remove(handler);
    }

    private static final class HandlerContext {

        private final Event.Type type;
        private final Object source;
        private final EventHandler handler;
        private final boolean add;

        public HandlerContext(final Type type, final Object source, final EventHandler handler, final boolean add) {
            this.type = type;
            this.source = source;
            this.handler = handler;
            this.add = add;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            else if (o == null || getClass() != o.getClass()) return false;
            else {
                final HandlerContext that = (HandlerContext) o;
                return add == that.add && Objects.equals(type, that.type) && Objects.equals(source, that.source)
                        && Objects.equals(handler, that.handler);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(add, type, source, handler);
        }
    }

}
