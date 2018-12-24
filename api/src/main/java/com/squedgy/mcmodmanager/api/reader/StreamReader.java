package com.squedgy.mcmodmanager.api.reader;

import com.squedgy.utilities.abstracts.Reader;
import com.squedgy.utilities.interfaces.Formatter;

import java.util.stream.Stream;

public class StreamReader <ReturnType, StreamType> extends Reader<ReturnType, Stream<StreamType>> {

    private final Stream<StreamType> stream;

    public StreamReader(Formatter<ReturnType, Stream<StreamType>> formatter, Stream<StreamType> stream) {
        super(formatter);
        this.stream = stream;
    }

    @Override
    public ReturnType read() throws Exception {
        return formatter.decode(stream);
    }
}
