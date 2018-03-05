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

package com.ponysdk.core.server.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.main.EntryPoint;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ApplicationManagerOption options;

    protected AbstractApplicationManager(final ApplicationManagerOption options) {
        this.options = options;
        log.info(options.toString());
    }

    public void startApplication(final TxnContext txnContext) throws Exception {
        final UIContext uiContext = txnContext.getUIContext();
        uiContext.execute(() -> {
            try {
                final EntryPoint entryPoint = initializeUIContext(uiContext);
                final String historyToken = txnContext.getHistoryToken();

                if (historyToken != null && !historyToken.isEmpty()) uiContext.getHistory().newItem(historyToken, false);

                entryPoint.start(uiContext);
            } catch (final Exception e) {
                log.error("Cannot start UIContext", e);
                // TODO nciaravola destroy if exception ?
            }
        });
    }

    protected abstract EntryPoint initializeUIContext(final UIContext ponySession) throws Exception;

    public ApplicationManagerOption getOptions() {
        return options;
    }

}
