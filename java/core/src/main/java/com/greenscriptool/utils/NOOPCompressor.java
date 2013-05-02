package com.greenscriptool.utils;

import java.io.Reader;
import java.io.Writer;

public class NOOPCompressor implements ICompressor {

    public void compress(Reader r, Writer w) throws Exception {
        char[] buffer = new char[1024];
        int n = 0;
        while (-1 != (n = r.read(buffer))) {
            w.write(buffer, 0, n);
        }
    }
}
