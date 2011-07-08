package se.skl.tp.vp.util;

import java.io.OutputStream;

public class StringBufferOutputStream extends OutputStream {

    private StringBuffer stringBuffer;

    public StringBufferOutputStream(StringBuffer stringBuffer) {
        this.stringBuffer = stringBuffer;
    }

    public void write(int character) {
        stringBuffer.append((char)character);
    }
}



