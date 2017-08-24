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

import java.util.Date;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DateConverter;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTDatePicker extends PTWidget<DatePicker> {

    private static final String DATE_SEPARATOR = ",";

    private final DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd");

    @Override
    protected DatePicker createUIObject() {
        return new DatePicker();
    }

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        addHandlers(uiService);
    }

    private void addHandlers(final UIBuilder uiService) {
        uiObject.addValueChangeHandler(event -> triggerEvent(uiObject, uiService, event));
        uiObject.addShowRangeHandler(event -> {
            final PTInstruction instruction = new PTInstruction(getObjectID());
            instruction.put(ClientToServerModel.HANDLER_SHOW_RANGE);
            instruction.put(ClientToServerModel.START_DATE, event.getStart().getTime());
            instruction.put(ClientToServerModel.END_DATE, event.getEnd().getTime());
            uiService.sendDataToServer(uiObject, instruction);
        });
    }

    protected void triggerEvent(final DatePicker picker, final UIBuilder uiService, final ValueChangeEvent<Date> event) {
        long date = -1;
        int year = -1;
        int month = -1;
        int day = -1;

        if (event.getValue() != null) {
            date = event.getValue().getTime();
            final String[] values = format.format(event.getValue()).split("-");
            year = Integer.parseInt(values[0]);
            month = Integer.parseInt(values[1]);
            day = Integer.parseInt(values[2]);
        }

        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE, date);
        instruction.put(ClientToServerModel.YEAR, year);
        instruction.put(ClientToServerModel.MONTH, month);
        instruction.put(ClientToServerModel.DAY, day);
        uiService.sendDataToServer(picker, instruction);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.DATE.ordinal() == modelOrdinal) {
            uiObject.setValue(DateConverter.fromTimestamp(binaryModel.getLongValue()));
            return true;
        } else if (ServerToClientModel.TIME.ordinal() == modelOrdinal) {
            uiObject.setCurrentMonth(DateConverter.fromTimestamp(binaryModel.getLongValue()));
            return true;
        } else if (ServerToClientModel.DATE_ENABLED.ordinal() == modelOrdinal) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.ENABLED
            final boolean enabled = buffer.readBinaryModel().getBooleanValue();
            for (final String date : dates) {
                uiObject.setTransientEnabledOnDates(enabled, DateConverter.decode(date));
            }
            return true;
        } else if (ServerToClientModel.ADD_DATE_STYLE.ordinal() == modelOrdinal) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.readBinaryModel().getStringValue();
            for (final String date : dates) {
                uiObject.addStyleToDates(style, DateConverter.decode(date));
            }
            return true;
        } else if (ServerToClientModel.REMOVE_DATE_STYLE.ordinal() == modelOrdinal) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.readBinaryModel().getStringValue();
            for (final String date : dates) {
                uiObject.removeStyleFromDates(style, DateConverter.decode(date));
            }
            return true;
        } else if (ServerToClientModel.YEAR_ARROWS_VISIBLE.ordinal() == modelOrdinal) {
            uiObject.setYearArrowsVisible(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
