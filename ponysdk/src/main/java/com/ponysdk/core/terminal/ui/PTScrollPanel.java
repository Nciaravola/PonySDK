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

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTScrollPanel extends PTSimplePanel {

    private boolean dragging;

    @Override
    protected ScrollPanel createUIObject() {
        return new ScrollPanel();
    }

    @Override
    public ScrollPanel cast() {
        return (ScrollPanel) uiObject;
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);
        uiObject.setWidget(w);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.HORIZONTAL_SCROLL_POSITION.equals(binaryModel.getModel())) {
            cast().setHorizontalScrollPosition(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.VERTICAL_SCROLL_POSITION.equals(binaryModel.getModel())) {
            cast().setVerticalScrollPosition(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.SCROLL_TO.equals(binaryModel.getModel())) {
            final long scrollTo = binaryModel.getLongValue();
            if (scrollTo == 0) cast().scrollToBottom();
            else if (scrollTo == 1) cast().scrollToLeft();
            else if (scrollTo == 2) cast().scrollToRight();
            else if (scrollTo == 3) cast().scrollToTop();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_SCROLL.equals(handlerModel)) {
            cast().addScrollHandler(new ScrollHandler() {

                @Override
                public void onScroll(final ScrollEvent event) {
                    if (!dragging) {
                        sendScrollPositionEvent(uiService);
                    }
                }
            });
            cast().addDomHandler(new MouseDownHandler() {

                @Override
                public void onMouseDown(final MouseDownEvent event) {
                    if (DOM.getCaptureElement() == null) {
                        dragging = true;
                        DOM.setCapture(cast().getElement());
                    }
                }
            }, MouseDownEvent.getType());
            cast().addDomHandler(new MouseUpHandler() {

                @Override
                public void onMouseUp(final MouseUpEvent event) {
                    dragging = false;
                    DOM.releaseCapture(uiObject.getElement());

                    sendScrollPositionEvent(uiService);
                }
            }, MouseUpEvent.getType());
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    private void sendScrollPositionEvent(final UIBuilder uiService) {
        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.HANDLER_SCROLL);
        eventInstruction.put(ClientToServerModel.HANDLER_SCROLL_HEIGHT, cast().getMaximumVerticalScrollPosition());
        eventInstruction.put(ClientToServerModel.HANDLER_SCROLL_WIDTH, cast().getMaximumHorizontalScrollPosition());
        eventInstruction.put(ClientToServerModel.HANDLER_SCROLL_VERTICAL, cast().getVerticalScrollPosition());
        eventInstruction.put(ClientToServerModel.HANDLER_SCROLL_HORIZONTAL, cast().getHorizontalScrollPosition());
        uiService.sendDataToServer(uiObject, eventInstruction);
    }

}
