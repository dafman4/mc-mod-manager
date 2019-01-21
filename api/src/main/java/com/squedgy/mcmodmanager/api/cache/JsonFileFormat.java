package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squedgy.utilities.interfaces.FileFormatter;

import java.io.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class JsonFileFormat implements FileFormatter<Map<String, String>> {

	private String workingFile = "";

	private JsonNode writeNode(Map.Entry<String,String> conf, ObjectNode root){
		String key = conf.getKey();
		int ind = key.indexOf('.');
		if(ind >= 0){
			String baseKey = key.substring(0, ind);
			Map.Entry<String,String> next = new AbstractMap.SimpleEntry<>(key.substring(ind + 1), conf.getValue());
			if(root.get(baseKey) == null || !(root.get(baseKey) instanceof ObjectNode)) root.set(baseKey,writeNode(next, JsonNodeFactory.instance.objectNode()));
			else root.set(key.substring(0, ind),writeNode(next, (ObjectNode) root.get(baseKey)));
		}else{
			root.put(key, conf.getValue());
		}
		return root;
	}

	@Override
	public Void encode(Map<String, String> conf) {
		ObjectNode ret = JsonNodeFactory.instance.objectNode();

		for(Map.Entry<String,String> entry : conf.entrySet()){
			writeNode(entry, ret);
		}
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
	 *
	 * @param conf       - the Map we're loading with all the nodes
	 * @param read       - The Node we're currently reading
	 * @param keyPrepend - A string that tells what nodes came before the current one, seperated by .'s
	 */
	private void readNode(Map<String, String> conf, ObjectNode read, String keyPrepend) {
		read
			.fields()
			.forEachRemaining(node -> {
				if (node.getValue().isObject())
					readNode(conf, (ObjectNode) node.getValue(), keyPrepend + node.getKey() + ".");

				else if (node.getValue().isTextual()) conf.put(keyPrepend + node.getKey(), node.getValue().textValue());
			});
	}

	@Override
	public Map<String, String> decode(Void ignored) {
		try (BufferedReader reader = new BufferedReader(new FileReader(getFile()))) {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode root = mapper.readValue(getFile(), ObjectNode.class);
			Map<String, String> ret = new HashMap<>();
			readNode(ret, root, "");
			return ret;
		} catch (IOException e) {
			throw new RuntimeException("There was an issue reading " + workingFile + ", please confirm it is a .json file, and that it's correctly formatted!", e);
		}
	}

	@Override
	public boolean shouldCreateFiles() {
		return true;
	}

	@Override
	public boolean isAppending() {
		return false;
	}

	@Override
	public void setWorkingFile(String s) {
		this.workingFile = new File(s).getAbsolutePath();
	}

	private File getFile() throws IOException {
		File ret = new File(workingFile);
		if (ret.exists()) {
			return ret;
		} else if (shouldCreateFiles()) {
			makeFile();
			return getFile();
		}
		throw new FileNotFoundException("File " + workingFile + " wasn't found!");
	}

	private void makeFile() throws IOException {
		File f = new File(workingFile),
			parent = f.toPath().getParent().toFile();
		if ((parent.exists() || parent.mkdirs()) && f.createNewFile()) {
			try (FileWriter writer = new FileWriter(f)) {
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
