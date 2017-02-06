/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.server.stm.Txn;

public class PCookies {

    private static final int ID = 0; // reserved

    private final Map<String, String> cachedCookies = new HashMap<>();

    private boolean isInitialized = false;

    public interface CookiesListener {

        void onInitialized();
    }

    private CookiesListener listener;

    public PCookies() {
    }

    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    public String removeCookie(final String name) {
        final WebsocketEncoder encoder = Txn.get().getEncoder();
        encoder.beginObject();
        encoder.encode(ServerToClientModel.TYPE_UPDATE, ID);
        encoder.encode(ServerToClientModel.REMOVE_COOKIE, name);
        encoder.endObject();

        return cachedCookies.remove(name);
    }

    public void setCookie(final String name, final String value) {
        setCookie(name, value, null);
    }

    public void setCookie(final String name, final String value, final Date expires) {
        cachedCookies.put(name, value);

        final WebsocketEncoder encoder = Txn.get().getEncoder();
        encoder.beginObject();
        encoder.encode(ServerToClientModel.TYPE_UPDATE, ID);
        encoder.encode(ServerToClientModel.ADD_COOKIE, name);
        encoder.encode(ServerToClientModel.VALUE, value);
        if (expires != null) encoder.encode(ServerToClientModel.COOKIE_EXPIRE, expires.getTime());
        encoder.endObject();
    }

    public void onClientData(final JsonObject event) {
        final JsonArray cookies = event.getJsonArray(ClientToServerModel.COOKIES.toStringValue());

        for (int i = 0; i < cookies.size(); i++) {
            final JsonObject object = cookies.getJsonObject(i);

            final String key = object.getString(ClientToServerModel.COOKIE_NAME.toStringValue());
            final String value = object.getString(ClientToServerModel.COOKIE_VALUE.toStringValue());

            cachedCookies.put(key, value);
        }

        if (!isInitialized) {
            isInitialized = true;
            if (listener != null) listener.onInitialized();
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setListener(final CookiesListener listener) {
        this.listener = listener;
    }

    public Collection<String> getNames() {
        return cachedCookies.keySet();
    }
}
