package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.api.cache.JsonFileFormat;
import com.squedgy.utilities.interfaces.FileFormatter;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonFileFormatTest {

	@Test
	public void testJsonCanWriteAndRead() throws Exception {
		final String singleDeep = "test",
			doubleDeep = "test123.abaa",
			tripleDeep = "test123.ab.c";
		final String singleWrite = "ace in the hole",
			doubleWrite = "test",
			tripleWrite = "triple";

		FileFormatter<Map<String, String>> format = new JsonFileFormat();

		FileWriter<Map<String, String>> writer = new FileWriter<>("test/test.json", format, false);

		Map<String, String> map = new HashMap<>();
		map.put(singleDeep, singleWrite);
		map.put(doubleDeep, doubleWrite);
		map.put(tripleDeep, tripleWrite);

		writer.write(map);

		map = new FileReader<>(format).read("test/test.json");
		Assert.assertEquals(singleWrite, map.get(singleDeep));
		Assert.assertEquals(doubleWrite, map.get(doubleDeep));
		Assert.assertEquals(tripleWrite, map.get(tripleDeep));
	}

}
