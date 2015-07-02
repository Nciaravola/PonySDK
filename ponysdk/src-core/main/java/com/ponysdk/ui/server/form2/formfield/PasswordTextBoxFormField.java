
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;

public class PasswordTextBoxFormField<T> extends TextBoxFormField<T> {

    public PasswordTextBoxFormField() {
        this(new PPasswordTextBox(), null);
    }

    public PasswordTextBoxFormField(final DataConverter<String, T> dataProvider) {
        this(new PPasswordTextBox(), dataProvider);
    }

    public PasswordTextBoxFormField(final PPasswordTextBox textBox, final DataConverter<String, T> dataProvider) {
        super(textBox, dataProvider);
    }

    @Override
    public PTextBox getTextBox() {
        return textBox;
    }

}
