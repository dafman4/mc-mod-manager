package com.squedgy.mcmodmanager.api.reader;

import com.squedgy.utilities.interfaces.Formatter;

import java.io.InputStream;
import java.util.Map;

public class JsonFormatter implements Formatter<Map<String,String>, InputStream> {


    @Override
    public InputStream encode(Map<String, String> stringStringMap) {
        return null;
    }

    @Override
    public Map<String, String> decode(InputStream stream) {
        return null;
    }
}
