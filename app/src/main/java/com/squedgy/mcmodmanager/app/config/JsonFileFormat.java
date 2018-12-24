package com.squedgy.mcmodmanager.app.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squedgy.utilities.interfaces.FileFormatter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JsonFileFormat implements FileFormatter<Map<String, String>> {

    private String workingFile = "";

    @Override
    public Void encode(Map<String, String> conf) {
        ObjectNode ret = JsonNodeFactory.instance.objectNode();

        conf
            .entrySet()
            .forEach( entry -> {
                String[] keys = entry.getKey().split("\\.");
                ObjectNode toModify = ret;
                for(String key: keys){
                    if(toModify.has(key)){
                        if(toModify.get(key).isObject()) toModify = (ObjectNode) toModify.get(key);
                        else toModify.putObject("key");
                    }else if(key.equals(keys[keys.length-1])){
                        toModify.put(key, entry.getValue());
                    } else toModify = toModify.putObject(key);
                }
            });
        ObjectMapper mapper = new ObjectMapper();
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();

        DefaultPrettyPrinter.Indenter i = new DefaultIndenter("\t", System.lineSeparator());

        printer.indentArraysWith(i);
        printer.indentObjectsWith(i);
        ObjectWriter writer = mapper.writer(printer);
        try {
            writer.writeValue(getFile(), ret);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't write to file: " + workingFile, e);
        }

        return null;
    }

    /**
     * An actually good reason to use recursion :D
     * @param conf - the Map we're loading with all the nodes
     * @param read - The Node we're currently reading
     * @param keyPrepend - A string that tells what nodes came before the current one, seperated by .'s
     */
    private void readNode(Map<String,String> conf, ObjectNode read, String keyPrepend){
        read
            .fields()
            .forEachRemaining( node -> {
                if(node.getValue().isObject()) readNode(conf, (ObjectNode) node.getValue(), keyPrepend + node.getKey() + ".");

                else if(node.getValue().isTextual()) conf.put(keyPrepend + node.getKey(), node.getValue().textValue());
            });
    }

    @Override
    public Map<String, String> decode(Void ignored) {
        try(BufferedReader reader = new BufferedReader(new FileReader(getFile()))){
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.readValue(getFile(), ObjectNode.class);
            Map<String,String> ret = new HashMap<>();
            readNode(ret, root, "");
            return ret;
        } catch (IOException e) {
            throw new RuntimeException("There was an issue reading " + workingFile + ", please confirm it is a .json file, and that it's correctly formatted!", e);
        }
    }

    @Override
    public boolean shouldCreateFiles() { return true; }

    @Override
    public boolean isAppending() { return false; }

    @Override
    public void setWorkingFile(String s) { this.workingFile = new File(s).getAbsolutePath(); }

    private File getFile() throws IOException{
        File ret = new File(workingFile);
        if (ret.exists()) {
            return ret;
        } else if (shouldCreateFiles()){
            makeFile();
            return getFile();
        }
        throw new FileNotFoundException("File " + workingFile + " wasn't found!");
    }

    private void makeFile() throws IOException {
        File f = new File(workingFile),
            parent = f.toPath().getParent().toFile();
        if ((parent.exists() || parent.mkdirs()) && f.createNewFile()) {
            try(FileWriter writer = new FileWriter(f)){
                writer.write(
                        "{\n" +
                        "}"
                );
                writer.flush();
            }
        } else {
          throw new IOException("There was an issue making " + f.getAbsolutePath());
        }
    }

}
