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

package com.ponysdk.core.terminal.model;

import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.core.model.ArrayValueModel;
import com.ponysdk.core.model.BooleanModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.ArrayBufferView;
import elemental.html.DataView;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class ReaderBuffer {

    public static final int NOT_FULL_BUFFER_POSITION = -1;

    private static final boolean LITTLE_INDIAN = false;

    private final BinaryModel currentBinaryModel;

    private DataView dataView;

    private Uint8Array buffer;

    private int position;

    private int modelSize;

    private int size;

    private Window window;

    public ReaderBuffer() {
        this.currentBinaryModel = new BinaryModel();
    }

    public void init(final Uint8Array buffer) {
        if (this.buffer != null && position < size) {
            if (this.window == null) {
                this.window = Browser.getWindow();
                createSetElementsMethodOnUint8Array();
            }
            final int remaningBufferSize = this.size - this.position;
            final Uint8Array mergedBuffer = window.newUint8Array(remaningBufferSize + buffer.getByteLength());
            mergedBuffer.setElements(this.position == 0 ? this.buffer : this.buffer.subarray(this.position), 0);
            mergedBuffer.setElements(buffer, remaningBufferSize);

            this.buffer = mergedBuffer;
        } else {
            this.buffer = buffer;
        }

        this.position = 0;
        this.size = this.buffer.getByteLength();
        this.dataView = newDataView(this.buffer.getBuffer(), this.buffer.getByteOffset(), this.size);
    }

    // WORKAROUND : No setElements on Uint8Array but Elemental need it, create a passthrough
    private static final native void createSetElementsMethodOnUint8Array() /*-{
                                                                           Uint8Array.prototype.setElements = function(array, offset) { this.set(array, offset) };
                                                                           }-*/;

    private static final native String decode(ArrayBufferView buffer, int position, int size) /*-{
                                                                                                    return $wnd.decode(buffer, position, size);
                                                                                                    }-*/;

    private static final native String fromCharCode(Uint8Array buffer) /*-{
                                                                       return String.fromCharCode.apply(null, buffer);
                                                                       }-*/;

    private static final native DataView newDataView(ArrayBuffer buffer, int byteOffset,
                                                     int length) /*-{ return new DataView(buffer, byteOffset, length); }-*/;

    private static final native int getUint8(DataView dataView, int position) /*-{ return dataView.getUint8(position); }-*/;

    private static final native int getInt8(DataView dataView, int position) /*-{ return dataView.getInt8(position); }-*/;

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public BinaryModel readBinaryModel() {
        final ServerToClientModel key = getModelKey();
        modelSize = getModelKeySize();

        final ValueTypeModel typeModel = key.getTypeModel();

        if (ValueTypeModel.INTEGER == typeModel) {
            modelSize += ValueTypeModel.INTEGER_SIZE;
            currentBinaryModel.init(key, getInt(), modelSize);
        } else if (ValueTypeModel.STRING == typeModel) {
            currentBinaryModel.init(key, readStringModelValue(), modelSize);
        } else if (ValueTypeModel.JSON_OBJECT == typeModel) {
            currentBinaryModel.init(key, readJsonModelValue(), modelSize);
        } else if (ValueTypeModel.NULL == typeModel) {
            currentBinaryModel.init(key, modelSize);
        } else if (ValueTypeModel.BOOLEAN == typeModel) {
            modelSize += ValueTypeModel.BOOLEAN_SIZE;
            currentBinaryModel.init(key, getBoolean(), modelSize);
        } else if (ValueTypeModel.BYTE == typeModel) {
            modelSize += ValueTypeModel.BYTE_SIZE;
            currentBinaryModel.init(key, getByte(), modelSize);
        } else if (ValueTypeModel.SHORT == typeModel) {
            modelSize += ValueTypeModel.SHORT_SIZE;
            currentBinaryModel.init(key, getShort(), modelSize);
        } else if (ValueTypeModel.DOUBLE == typeModel) {
            modelSize += ValueTypeModel.DOUBLE_SIZE;
            currentBinaryModel.init(key, getDouble(), modelSize);
        } else if (ValueTypeModel.LONG == typeModel) {
            modelSize += ValueTypeModel.LONG_SIZE;
            currentBinaryModel.init(key, getLong(), modelSize);
        } else if (ValueTypeModel.FLOAT == typeModel) {
            modelSize += ValueTypeModel.FLOAT_SIZE;
            currentBinaryModel.init(key, getFloat(), modelSize);
        } else if (ValueTypeModel.ARRAY == typeModel) {
            currentBinaryModel.init(key, readArrayModelValue(), modelSize);
        } else {
            // Never have to happen
            throw new IllegalArgumentException("Unknown type model : " + typeModel);
        }

        return currentBinaryModel;
    }

    private void checkRemainingBytes(final int bytes) {
        if (!hasEnoughRemainingBytes(bytes)) throw new ArrayIndexOutOfBoundsException();
    }

    private boolean getBoolean() {
        checkRemainingBytes(ValueTypeModel.BOOLEAN_SIZE);
        return buffer.intAt(position++) == BooleanModel.TRUE.ordinal();
    }

    private int getByte() {
        checkRemainingBytes(ValueTypeModel.BYTE_SIZE);
        return getInt8(dataView, position++);
    }

    private int getUnsignedByte() {
        checkRemainingBytes(ValueTypeModel.BYTE_SIZE);
        return getUint8(dataView, position++);
    }

    private int getShort() {
        checkRemainingBytes(ValueTypeModel.SHORT_SIZE);
        final int result = dataView.getInt16(position);
        position += ValueTypeModel.SHORT_SIZE;
        return result;
    }

    private int getUnsignedShort() {
        checkRemainingBytes(ValueTypeModel.SHORT_SIZE);
        final int result = dataView.getUint16(position);
        position += ValueTypeModel.SHORT_SIZE;
        return result;
    }

    private int getInt() {
        checkRemainingBytes(ValueTypeModel.INTEGER_SIZE);
        final int result = dataView.getInt32(position);
        position += ValueTypeModel.INTEGER_SIZE;
        return result;
    }

    private long getUnsignedInt() {
        checkRemainingBytes(ValueTypeModel.INTEGER_SIZE);
        final int result = dataView.getUint32(position);
        position += ValueTypeModel.INTEGER_SIZE;
        return result;
    }

    private long getLong() {
        checkRemainingBytes(ValueTypeModel.LONG_SIZE);
        final long result = ((long) dataView.getInt32(position, LITTLE_INDIAN) << 32)
                + (dataView.getInt32(position + ValueTypeModel.INTEGER_SIZE, LITTLE_INDIAN) & 0xFFFFFFFFL);
        position += ValueTypeModel.LONG_SIZE;
        return result;
    }

    private float getFloat() {
        checkRemainingBytes(ValueTypeModel.FLOAT_SIZE);
        final float value = dataView.getFloat32(position, LITTLE_INDIAN);
        position += ValueTypeModel.FLOAT_SIZE;
        return value;
    }

    private double getDouble() {
        checkRemainingBytes(ValueTypeModel.DOUBLE_SIZE);
        final double value = dataView.getFloat64(position, LITTLE_INDIAN);
        position += ValueTypeModel.DOUBLE_SIZE;
        return value;
    }

    private String readStringModelValue() {
        modelSize += ValueTypeModel.BYTE_SIZE;
        int stringLength = getUnsignedByte();
        boolean ascii = true;
        if (stringLength > ValueTypeModel.STRING_ASCII_UINT8_MAX_LENGTH) {
            if (stringLength == ValueTypeModel.STRING_ASCII_UINT16) {
                modelSize += ValueTypeModel.SHORT_SIZE;
                stringLength = getUnsignedShort();
            } else if (stringLength == ValueTypeModel.STRING_ASCII_INT32) {
                modelSize += ValueTypeModel.INTEGER_SIZE;
                stringLength = getInt();
            } else {
                ascii = false;
                if (stringLength == ValueTypeModel.STRING_UTF8_UINT8) {
                    modelSize += ValueTypeModel.BYTE_SIZE;
                    stringLength = getUnsignedByte();
                } else if (stringLength == ValueTypeModel.STRING_UTF8_UINT16) {
                    modelSize += ValueTypeModel.SHORT_SIZE;
                    stringLength = getUnsignedShort();
                } else {
                    modelSize += ValueTypeModel.INTEGER_SIZE;
                    stringLength = getInt();
                }
            }
        }
        modelSize += stringLength;
        return getString(ascii, stringLength);
    }

    private JSONObject readJsonModelValue() {
        final String s = readStringModelValue();
        try {
            return s != null ? JSONParser.parseStrict(s).isObject() : null;
        } catch (final JSONException e) {
            throw new JSONException(e.getMessage() + " : " + s, e);
        }
    }

    private Object[] readArrayModelValue() {
        modelSize += ValueTypeModel.BYTE_SIZE; //array size
        final Object[] array = new Object[getUnsignedByte()];
        modelSize += array.length; //array elements types
        for (int i = 0; i < array.length; i++) {
            final ArrayValueModel arrayValueModel = ArrayValueModel.fromRawValue(getByte());
            modelSize += arrayValueModel.getMinSize();
            if (arrayValueModel.isDynamicSize()) {
                array[i] = getDynamicSizeArrayElement(arrayValueModel);
            } else if (arrayValueModel == ArrayValueModel.NULL) {
                array[i] = null;
            } else if (arrayValueModel == ArrayValueModel.INTEGER) {
                array[i] = getInt();
            } else if (arrayValueModel == ArrayValueModel.SHORT) {
                array[i] = getShort();
            } else if (arrayValueModel == ArrayValueModel.BOOLEAN_FALSE) {
                array[i] = false;
            } else if (arrayValueModel == ArrayValueModel.BOOLEAN_TRUE) {
                array[i] = true;
            } else if (arrayValueModel == ArrayValueModel.LONG) {
                array[i] = getLong();
            } else if (arrayValueModel == ArrayValueModel.BYTE) {
                array[i] = getByte();
            } else if (arrayValueModel == ArrayValueModel.DOUBLE) {
                array[i] = getDouble();
            } else if (arrayValueModel == ArrayValueModel.FLOAT) {
                array[i] = getFloat();
            } else {
                throw new IllegalArgumentException("Unsupported ArrayValueModel " + arrayValueModel);
            }
        }
        return array;
    }

    private Object getDynamicSizeArrayElement(final ArrayValueModel arrayValueModel) {
        final int msgSize = getArrayElementDynamicSize(arrayValueModel.getMinSize());
        boolean ascii;
        if (arrayValueModel == ArrayValueModel.STRING_ASCII_UINT8_LENGTH
                || arrayValueModel == ArrayValueModel.STRING_ASCII_UINT16_LENGTH) {
            ascii = true;
        } else if (arrayValueModel == ArrayValueModel.STRING_UTF8_UINT8_LENGTH
                || arrayValueModel == ArrayValueModel.STRING_UTF8_UINT16_LENGTH
                || arrayValueModel == ArrayValueModel.STRING_UTF8_INT32_LENGTH) {
            ascii = false;
        } else {
            throw new IllegalArgumentException("Unsupported ArrayValueModel " + arrayValueModel);
        }
        return ascii ? decodeStringAscii(msgSize) : decodeStringUTF8(msgSize);
    }

    private String getStringAscii(final int size) {
        if (size != 0) {
            return decodeStringAscii(size);
        } else {
            return null;
        }
    }

    private String getString(final boolean ascii, final int size) {
        if (size != 0) {
            return size < 100_000 && ascii ? decodeStringAscii(size) : decodeStringUTF8(size);
        } else {
            return null;
        }
    }

    private String decodeStringAscii(final int size) {
        checkRemainingBytes(size);
        final String result = fromCharCode(buffer.subarray(position, position + size));
        position += size;
        return result;
    }

    private String decodeStringUTF8(final int size) {
        checkRemainingBytes(size);
        final String result = decode(buffer, position, position + size);
        position += size;
        return result;
    }

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

    public boolean hasEnoughKeyBytes() {
        return hasEnoughRemainingBytes(getModelKeySize());
    }

    public boolean hasEnoughRemainingBytes(final int blockSize) {
        return position + blockSize <= size;
    }

    /**
     * Go directly to the next block
     *
     * @param dryRun
     *            If true, not really shift
     * @return Start position of the next block
     */
    public int shiftNextBlock(final boolean dryRun) {
        final int startPosition = position;
        int endPosition = NOT_FULL_BUFFER_POSITION;
        while (hasEnoughKeyBytes()) {
            try {
                final ServerToClientModel currentKeyModel = shiftBinaryModel();
                if (ServerToClientModel.END == currentKeyModel) {
                    endPosition = position;
                    break;
                }
            } catch (final ArrayIndexOutOfBoundsException e) {
                // No more enough bytes
                break;
            }
        }

        // No end found, it's a split message, so we rewind
        // If it's a dry run, we rewind all the time
        if (endPosition == NOT_FULL_BUFFER_POSITION || dryRun) position = startPosition;

        return endPosition;
    }

    private final ServerToClientModel shiftBinaryModel() {
        final ServerToClientModel key = getModelKey();

        final ValueTypeModel typeModel = key.getTypeModel();

        if (ValueTypeModel.INTEGER == typeModel) {
            position += ValueTypeModel.INTEGER_SIZE;
        } else if (ValueTypeModel.STRING == typeModel || ValueTypeModel.JSON_OBJECT == typeModel) {
            shiftString();
        } else if (ValueTypeModel.NULL == typeModel) {
            // Nothing to do
        } else if (ValueTypeModel.BOOLEAN == typeModel) {
            position += ValueTypeModel.BOOLEAN_SIZE;
        } else if (ValueTypeModel.BYTE == typeModel) {
            position += ValueTypeModel.BYTE_SIZE;
        } else if (ValueTypeModel.SHORT == typeModel) {
            position += ValueTypeModel.SHORT_SIZE;
        } else if (ValueTypeModel.DOUBLE == typeModel) {
            position += ValueTypeModel.DOUBLE_SIZE;
        } else if (ValueTypeModel.LONG == typeModel) {
            position += ValueTypeModel.LONG_SIZE;
        } else if (ValueTypeModel.FLOAT == typeModel) {
            position += ValueTypeModel.FLOAT_SIZE;
        } else if (ValueTypeModel.ARRAY == typeModel) {
            shiftArray();
        } else {
            throw new IllegalArgumentException("Unknown type model : " + typeModel);
        }

        return key;
    }

    private void shiftString() {
        int messageSize = getUnsignedByte();
        if (messageSize > ValueTypeModel.STRING_ASCII_UINT8_MAX_LENGTH) {
            if (messageSize == ValueTypeModel.STRING_UTF8_UINT8) {
                messageSize = getUnsignedByte();
            } else if (messageSize == ValueTypeModel.STRING_ASCII_UINT16 || messageSize == ValueTypeModel.STRING_UTF8_UINT16) {
                messageSize = getUnsignedShort();
            } else {
                messageSize = getInt();
            }
        }
        position += messageSize;
    }

    private void shiftArray() {
        final int arrayLength = getUnsignedByte();
        for (int i = 0; i < arrayLength; i++) {
            final ArrayValueModel arrayValueModel = ArrayValueModel.fromRawValue(getByte());
            if (arrayValueModel.isDynamicSize()) {
                final int msgLength = getArrayElementDynamicSize(arrayValueModel.getMinSize());
                position += msgLength;
            } else {
                position += arrayValueModel.getMinSize();
            }
        }
    }

    private int getArrayElementDynamicSize(final int lengthOfSize) {
        if (lengthOfSize == 1) {
            return getUnsignedByte();
        } else if (lengthOfSize == 2) {
            return getUnsignedShort();
        } else if (lengthOfSize == 4) {
            return getInt();
        } else {
            throw new IllegalArgumentException("Invalid Array element lengthOfSize " + lengthOfSize);
        }
    }

    /**
     * Get the model key
     */
    private ServerToClientModel getModelKey() {
        return ServerToClientModel.fromRawValue(getUnsignedByte());
    }

    /**
     * Get the model key size
     */
    private static final int getModelKeySize() {
        return ValueTypeModel.BYTE_SIZE;
    }

    /**
     * Slice the array [startPosition, endPosition[
     */
    public Uint8Array slice(final int startPosition, final int endPosition) {
        position = endPosition;
        return buffer.subarray(startPosition, endPosition);
    }

    @Override
    public String toString() {
        return "Buffer " + hashCode() + " ; position = " + position + " ; size = " + size;
    }

}
