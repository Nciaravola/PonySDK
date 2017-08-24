package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.HeaderRenderer;

public class PLabelHeaderRenderer implements HeaderRenderer {

    private final PLabel widget;

    public PLabelHeaderRenderer(final String caption) {
        widget = Element.newPLabel(caption);
    }

    @Override
    public PWidget render() {
        return widget;
    }

}
