package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.CacheRetrievalException;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.cache.CachingFailedException;
import com.squedgy.mcmodmanager.api.response.CurseForgeResponseDeserializer;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public abstract class ModChecker {

	private static String currentRead = "", currentWrite = "";

	public static CurseForgeResponse getForVersion(String mod, String version) throws ModIdFoundConnectionFailed, IOException {
		return get(mod, new CurseForgeResponseDeserializer(version));
	}

	public static CurseForgeResponse get(String mod) throws ModIdFoundConnectionFailed, IOException {
		return get(mod, new CurseForgeResponseDeserializer());
	}

	private static synchronized void setReadWrite(Runnable r) {
		r.run();
	}

	public static synchronized ModVersion getCurrentVersion(String mod, String mcVersion) throws CacheRetrievalException {
		ModVersion ret;
		while (currentWrite.equals(mod + "." + mcVersion)) ;
		try {
			setReadWrite(() -> currentRead = mod + "." + mcVersion);
			ret = Cacher.getInstance(mcVersion).getMod(mod);
		} catch (Exception e) {
			throw new CacheRetrievalException();
		} finally {
			setReadWrite(() -> currentRead = "");
		}
		if (ret == null) throw new CacheRetrievalException();
		return ret;
	}

	public static synchronized void writeCurrentVersion(ModVersion fromCurse, String mcVersion, String modId, String dotMinecraft) throws CachingFailedException {
		while (currentRead.equals(modId + "." + fromCurse)) ;
		try {
			setReadWrite(() -> currentWrite = modId + "." + fromCurse);
			File f = new File(dotMinecraft + File.separator + fromCurse.getFileName());
			if (f.exists()) {
				Cacher c = Cacher.getInstance(mcVersion);
				c.addMod(modId, fromCurse);
			}
		} catch (Exception e) {
			AppLogger.error(e, ModChecker.class);
			throw new CachingFailedException();
		} finally {
			setReadWrite(() -> currentWrite = "");
		}
	}

	private static CurseForgeResponse get(String mod, CurseForgeResponseDeserializer deserializer) throws IOException, ModIdFoundConnectionFailed {

		URL url = new URL("https://api.cfwidget.com/minecraft/mc-mods/" + mod);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();

		if (responseCode >= 400 && responseCode < 500) {
			url = new URL("https://api.cfwidget.com/mc-mods/minecraft/" + mod);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			responseCode = con.getResponseCode();
		}

		if (responseCode >= 400 && responseCode < 500) {
			url = new URL("https://api.cfwidget.com/projects/" + mod);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			responseCode = con.getResponseCode();
		}

		if(responseCode >= 400 && responseCode < 500) throw new IOException(mod + " couldn't be found/accessed");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			ObjectMapper mapper = new ObjectMapper()
				.registerModule(
					new SimpleModule()
						.addDeserializer(CurseForgeResponse.class, deserializer)
				);
			return mapper
				.readValue(
					reader.lines()
						.collect(Collectors.joining(""))
						.replaceAll("\\n", "\\n")
						.replaceAll("\\r", "\\r"),
					CurseForgeResponse.class
				);
		} catch (FileNotFoundException e) {
			throw new IOException(e.getMessage());
		} catch (Exception e) {
			throw new ModIdFoundConnectionFailed(String.format("Unknown Error with mod %s: %s", mod, e.getMessage()), e);
		}
	}

	public static ModVersion getNewest(String mId, String mcV) throws ModIdNotFoundException {
		try {
			CurseForgeResponse resp = getForVersion(mId, mcV);

			ModVersion ret = resp
				.getVersions()
				.stream()
				.max(Comparator.comparing(ModVersion::getUploadedAt))
				.orElse(null);
			new ObjectMapper().writeValue(new File(System.getProperty("user.home") + File.separator + "checker-debug" + File.separator + mId + ".json"), resp);
			if (ret != null) return ret;
		} catch (Exception ex) {
		}
		throw new ModIdNotFoundException("Couldn't find the mod Id : " + mId + ". It's not cached and DOESN'T match a Curse Forge mod. Talk to the mod author about having the Id within their mcmod.info file match their Curse Forge mod id.");
	}

	public static boolean download(ModVersion v, String location, String mcVersion) {
		try {
			URL u = new URL(v.getDownloadUrl() + "/file");

			HttpsURLConnection connection = (HttpsURLConnection) u.openConnection();
			connection.setRequestProperty("User-Agent", getUserAgentForOs());
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.connect();
			if (connection.getResponseCode() > 299 || connection.getResponseCode() < 200) {
				AppLogger.info("Couldn't access the url :" + u, ModChecker.class);
				connection.getHeaderFields().forEach((key, field) -> {
					AppLogger.info(key + ": " + field, ModChecker.class);
				});
				return false;
			}

			boolean append = !v.getFileName().endsWith(".jar");
			String path = (location + v.getFileName() + (append ? ".jar" : "")).replace('+', ' ');
			try (
				FileOutputStream outFile = new FileOutputStream(new File(path));
				ReadableByteChannel in = Channels.newChannel(connection.getInputStream());
				FileChannel out = outFile.getChannel()
			) {

				out.transferFrom(in, 0, Long.MAX_VALUE);
				connection.disconnect();
				return true;
			}
		}catch(Exception e){
			AppLogger.error(e, ModChecker.class);
			return false;
		}
	}

	private static String getUserAgentForOs(){
		String os = System.getProperty("os.name");
		if (os.matches(".*[Ww]indows.*")) return "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0";
		else if (os.matches(".*[Mm]ac [Oo][Ss].*")) return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:65.0) Gecko/20100101 Firefox/65.0";
		else return "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:65.0) Gecko/20100101 Firefox/65.0";
	}

}
