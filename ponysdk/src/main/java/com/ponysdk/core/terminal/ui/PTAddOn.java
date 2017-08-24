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

package com.ponysdk.core.terminal.ui;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.JavascriptAddOn;
import com.ponysdk.core.terminal.JavascriptAddOnFactory;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTAddOn extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTAddOn.class.getName());

    protected boolean destroyed;

    JavascriptAddOn addOn;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        doCreate(buffer, objectId, uiService);
    }

    protected void doCreate(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        // ServerToClientModel.FACTORY
        final String signature = buffer.readBinaryModel().getStringValue();
        final Map<String, JavascriptAddOnFactory> factories = uiService.getJavascriptAddOnFactory();

        final JavascriptAddOnFactory factory = factories.get(signature);
        if (factory == null) throw new IllegalArgumentException(
            "AddOn factory not found for signature: " + signature + ". Addons registered: " + factories.keySet());

        final JSONObject params = new JSONObject();
        params.put("id", new JSONNumber(objectId));

        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.NATIVE.equals(binaryModel.getModel())) params.put("args", binaryModel.getJsonObject());
        else buffer.rewind(binaryModel);

        try {
            addOn = factory.newAddOn(params.getJavaScriptObject());
            addOn.onInit();
        } catch (final JavaScriptException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.NATIVE.ordinal() == modelOrdinal) {
            doUpdate(binaryModel.getJsonObject());
            return true;
        } else if (ServerToClientModel.DESTROY.ordinal() == modelOrdinal) {
            destroy();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    protected void doUpdate(final JSONObject data) {
        try {
            if (!destroyed) addOn.update(data.getJavaScriptObject());
            else log.warning("PTAddOn #" + getObjectID() + " destroyed, so updates will be discarded : " + data.toString());
        } catch (final JavaScriptException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            addOn.destroy();
            destroyed = true;
        }
    }
}
