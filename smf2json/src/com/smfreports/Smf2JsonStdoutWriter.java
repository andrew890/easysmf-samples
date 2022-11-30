package com.smfreports;

import java.io.IOException;

class Smf2JsonStdoutWriter extends Smf2JsonWriter {

    @Override
    public void close() throws IOException {
        // nothing to do
        
    }

    @Override
    void writeLine(String output) throws IOException {
        System.out.println(output);
        
    }
}
