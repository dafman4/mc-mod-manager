package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModMember;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.CurseForgeResponseDeserializer;
import com.squedgy.utilities.interfaces.InputStreamFormatter;
import com.squedgy.utilities.reader.FileReader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static com.squedgy.utilities.Resources.getResource;
import static org.slf4j.LoggerFactory.getLogger;

public class DeserializerChecker {

	private static final LocalDateTime uploadedTime = LocalDateTime.of(2019, 4, 13, 21, 2, 35);
	private static final String url = "https://www.curseforge.com/minecraft/mc-mods/jei/download/2698788",
		name = "jei_1.12.2-4.15.0.276.jar",
		type = "beta",
		version = "1.12.2",
		title = "Owner",
		username = "mezz",
		modName = "Just Enough Items (JEI)",
		modId = "jei";
	private static final long id = 2698788;

	private static String JSON;

	private static final Logger log = getLogger(DeserializerChecker.class);

	private static ObjectMapper mapper;

	@BeforeClass
	public static void init() throws Exception {

		mapper = new ObjectMapper();
		mapper.registerModule(new SimpleModule().addDeserializer(CurseForgeResponse.class, new CurseForgeResponseDeserializer()));

		JSON = new FileReader<>(new InputStreamFormatter<String>() {
			@Override
			public InputStream encode(String s) throws IOException {
				return new ByteArrayInputStream(s.getBytes());
			}

			@Override
			public String decode(InputStream stream) throws IOException {
				StringBuilder builder = new StringBuilder();
				int c = stream.read();
				while(c != -1) {
					builder.append((char)c);
					c = stream.read();
				}
				return builder.toString();
			}
		}).read(Paths.get(DeserializerChecker.class.getResource("test-response.json").toURI()));

	}

	@Test
	public void testDeserializer() throws IOException {

		CurseForgeResponse response = mapper.readValue(JSON, CurseForgeResponse.class);
		Assert.assertEquals(156, response.getRelatedVersions("1.12.2").size());
		ModVersion v = response.getRelatedVersions("1.12.2").get(1);
		Assert.assertEquals(type, v.getTypeOfRelease());
		Assert.assertEquals(url, v.getDownloadUrl());
		Assert.assertEquals(name, v.getFileName());
		Assert.assertEquals(version, v.getMinecraftVersion());
		Assert.assertEquals(id, v.getId());
		Assert.assertEquals(uploadedTime, v.getUploadedAt());
		Assert.assertEquals(modId, v.getModId());
		Assert.assertEquals(modName, v.getModName());

		ModMember member = response.getMembers().get(0);

		Assert.assertEquals(title, member.getTitle());
		Assert.assertEquals(username, member.getUsername());

	}

	@Test
	public void testGetLatest() throws Exception {
		CurseForgeResponse response = mapper.readValue(JSON, CurseForgeResponse.class);
		Assert.assertEquals(LocalDateTime.of(2019, 4, 14, 20, 2 ,35), response.getLatestVersion("1.12.2").getUploadedAt());
	}

}
