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

package com.ponysdk.ui.terminal.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTDateBox.MyDateBox;

public class PTDateBox extends PTWidget<MyDateBox> {

    private static final DefaultFormat DEFAULT_FORMAT = GWT.create(DefaultFormat.class);

    @Override
    public void create(final PTInstruction create, final UIService uiService) {

        final PTDatePicker datePicker = (PTDatePicker) uiService.getPTObject(create.getLong(PROPERTY.PICKER));

        init(create, uiService, new MyDateBox(datePicker.cast(), null, DEFAULT_FORMAT));
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {

        if (HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER.equals(addHandler.getString(HANDLER.KEY))) {
            final DateBox dateBox = cast();
            final TextBox textBox = dateBox.getTextBox();
            dateBox.addValueChangeHandler(new ValueChangeHandler<Date>() {

                @Override
                public void onValueChange(final ValueChangeEvent<Date> event) {
                    triggerEvent(addHandler, uiService, dateBox);
                }

            });
            textBox.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    triggerEvent(addHandler, uiService, dateBox);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    protected void triggerEvent(final PTInstruction addHandler, final UIService uiService, final DateBox dateBox) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(addHandler.getObjectID());
        instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        instruction.put(HANDLER.KEY, HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER);
        instruction.put(PROPERTY.VALUE, dateBox.getTextBox().getText());

        uiService.sendDataToServer(dateBox, instruction);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final MyDateBox dateBox = cast();
        if (update.containsKey(PROPERTY.VALUE)) {
            dateBox.getTextBox().setValue(update.getString(PROPERTY.VALUE));
        } else if (update.containsKey(PROPERTY.DATE_FORMAT)) {
            final DefaultFormat format = new DefaultFormat(DateTimeFormat.getFormat(update.getString(PROPERTY.DATE_FORMAT)));
            dateBox.setFormat(format);
        } else if (update.containsKey(PROPERTY.DATE_FORMAT_PATTERN)) {
            dateBox.setFormat(new DefaultFormat(DateTimeFormat.getFormat(update.getString(PROPERTY.DATE_FORMAT_PATTERN))));
        } else if (update.containsKey(PROPERTY.ENABLED)) {
            dateBox.setEnabled(update.getBoolean(PROPERTY.ENABLED));
        } else if (update.containsKey(PROPERTY.MONTH)) {
            dateBox.setDefaultMonth(update.getString(PROPERTY.MONTH));
        } else {
            super.update(update, uiService);
        }
    }

    public static class MyDateBox extends DateBox {

        private Date defaultMonth = null;

        public MyDateBox(final DatePicker picker, final Date date, final Format format) {
            super(picker, date, format);
        }

        public void setDefaultMonth(final String m) {
            if (m.isEmpty()) defaultMonth = null;
            else defaultMonth = new Date(Long.parseLong(m));
        }

        @Override
        public void showDatePicker() {
            if (!getTextBox().getText().trim().isEmpty() || defaultMonth == null) {
                super.showDatePicker();
                return;
            }

            super.showDatePicker();
            getDatePicker().setCurrentMonth(defaultMonth);
        }

    }
}
