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
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTDateBox.MyDateBox;

public class PTDateBox extends PTWidget<MyDateBox> {

    private PTDatePicker datePicker;
    private DefaultFormat format;
    private boolean iskeepDayTimeNeeded = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        datePicker = (PTDatePicker) uiService.getPTObject(buffer.readBinaryModel().getIntValue());
        format = new DefaultFormat(DateTimeFormat.getFormat(buffer.readBinaryModel().getStringValue()));
        final BinaryModel keepTimeModel = buffer.readBinaryModel();
        if (keepTimeModel.getModel() == ServerToClientModel.KEEP_DAY_TIME_NEEDED) {
            this.iskeepDayTimeNeeded = true;
        } else {
            buffer.rewind(keepTimeModel);
        }
        super.create(buffer, objectId, uiService);
    }

    @Override
    protected MyDateBox createUIObject() {
        return new MyDateBox(datePicker.uiObject, null, format, iskeepDayTimeNeeded);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.VALUE == model) {
            uiObject.getTextBox().setValue(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.DATE_FORMAT_PATTERN == model) {
            format = new DefaultFormat(DateTimeFormat.getFormat(binaryModel.getStringValue()));
            uiObject.setFormat(format);
            return true;
        } else if (ServerToClientModel.ENABLED == model) {
            uiObject.setEnabled(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.TIME == model) {
            uiObject.setDefaultMonth(binaryModel.getLongValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    class MyDateBox extends DateBox {

        private static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
        private static final int ONE_DAY_IN_MILLIS = 24 * 60 * ONE_MINUTE_IN_MILLIS;

        private Date defaultMonth;
        private Date lastDate;

        private MyDateBox(final DatePicker picker, final Date date, final Format format, final boolean iskeepDayTimeNeeded) {
            super(picker, date, format);
            if (iskeepDayTimeNeeded) {
                getTextBox().addValueChangeHandler(this::onTextBoxChanged);
                getDatePicker().addValueChangeHandler(this::onDatePickerChanged);
            } else addValueChangeHandler(this::onDateChanged);
        }

        private void setDefaultMonth(final long m) {
            defaultMonth = new Date(m);
        }

        @Override
        public void showDatePicker() {
            super.showDatePicker();
            if (defaultMonth != null && getTextBox().getText().trim().isEmpty()) {
                getDatePicker().setCurrentMonth(defaultMonth);
            }
        }

        private void onTextBoxChanged(final ValueChangeEvent<String> event) {
            final Date textBoxDate = format.parse(this, event.getValue(), true);
            lastDate = textBoxDate;
            if (textBoxDate != null) fireDateChanged();
        }

        private void onDatePickerChanged(final ValueChangeEvent<Date> event) {
            Date pickerDate = event.getValue();
            if (lastDate != null) {
                final int dayTime = (int) (lastDate.getTime() % ONE_DAY_IN_MILLIS
                        - lastDate.getTimezoneOffset() * ONE_MINUTE_IN_MILLIS);
                final long dateInMillis = pickerDate.getTime() + dayTime;
                pickerDate = new Date(dateInMillis);
                getTextBox().setValue(format.format(this, pickerDate), false);
            }
            fireDateChanged();
        }

        private void onDateChanged(final ValueChangeEvent<Date> event) {
            fireDateChanged();
        }

        private void fireDateChanged() {
            final PTInstruction instruction = new PTInstruction(PTDateBox.this.getObjectID());
            instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, getTextBox().getValue());
            uiBuilder.sendDataToServer(this, instruction);
        }

    }
}
