package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.CurseForgeResponseDeserializer;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.*;
import java.net.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class ModChecker {

	private static final RedirectStrategy redirectionStrategy = new DefaultRedirectStrategy() {
		@Override
		protected URI createLocationURI(String location) throws ProtocolException {
			try {
				return buildURI(location);
			} catch (Exception e) {
				return null;
			}
		}
	};

	public static CurseForgeResponse getForVersion(String mod, String version) throws ModIdFoundConnectionFailed, IOException {
		return get(mod, new CurseForgeResponseDeserializer(version));
	}

	public static CurseForgeResponse get(String mod) throws ModIdFoundConnectionFailed, IOException {
		return get(mod, new CurseForgeResponseDeserializer());
	}

	private static CloseableHttpResponse connectInOrder(String... urls){
		System.out.println(urls.length);
		for(String url: urls){
			CloseableHttpClient client = getClient();
			System.out.println(url);
			HttpGet get = new HttpGet(url);
			try{
				CloseableHttpResponse resp = client.execute(get);
				int responseCode = resp.getStatusLine().getStatusCode();
				if(responseCode >= 200 && responseCode < 300){
					System.out.println("found at:" + url);
					return resp;
				}else {
					resp.close();
				}
			}catch(IOException e){ System.out.println(e.getMessage()); }
		}
		System.out.println("returning null");
		return null;
	}

	private static CurseForgeResponse get(String mod, CurseForgeResponseDeserializer deserializer) throws IOException, ModIdFoundConnectionFailed {
		System.out.println("Get called for mod: " + mod);
		try(CloseableHttpResponse response = connectInOrder(
			"https://api.cfwidget.com/minecraft/mc-mods/" + mod,
			"https://api.cfwidget.com/mc-mods/minecraft/" + mod,
			"https://api.cfwidget.com/projects/" + mod)
		){
			if(response == null){
				System.out.println("null response");
				throw new IOException(String.format("%s couldn't be found/accessed", mod));
			}
			System.out.println("response not null");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
				ObjectMapper mapper = new ObjectMapper()
					.registerModule(
						new SimpleModule()
							.addDeserializer(CurseForgeResponse.class, deserializer)
					);
				String text = reader.lines()
					.collect(Collectors.joining(""))
					.replaceAll("\\n", "\\\\n")
					.replaceAll("\\r", "\\\\r");
				System.out.println("response:\n" + response);
				return mapper
					.readValue(
						text,
						CurseForgeResponse.class
					);
			} catch (FileNotFoundException e) {
				throw new IOException(e.getMessage());
			} catch (Exception e) {
				throw new ModIdFoundConnectionFailed(String.format("Unknown Error with mod %s: %s", mod, e.getMessage()), e);
			}
		}catch (Exception e){
			AppLogger.error(e, ModChecker.class);
			throw e;
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
		throw new ModIdNotFoundException(
			String.format(
				"Couldn't find the mod Id : %s. It's doesn't match a Curse Forge mod. " +
				"Talk to the mod author about having the Id within their mcmod.info file match their Curse Forge mod id.",
				mId
			)
		);
	}

	private static URI buildURI(String url) throws URISyntaxException {
		int i = url.indexOf("://");
		String scheme = url.substring(0, i);
		url = url.substring(i + "://".length());
		int slash = url.indexOf('/');
		String host = url.substring(0, slash);
		String path = url.substring(slash);
		//The first one encodes, the second fixes any encoding encoded item issues
		//Things like Pam's Harvest Craft don't work without this...
		return new URI(new URI(scheme, host, path, null).toString().replaceAll("%25{2}([2-9A-Fa-f]{2})", "%$1"));
	}

	private static CloseableHttpClient getClient(){
		HttpClientBuilder builder = HttpClients.custom()
			.setRedirectStrategy(redirectionStrategy)
			.setDefaultRequestConfig(
				RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.build()
			);
		//If proxy then add it
		if (System.getProperty("https.proxyPort") != null && System.getProperty("https.proxyHost") != null) {
			builder.setProxy(new HttpHost(System.getProperty("https.proxyHost"), Integer.valueOf(System.getProperty("https.proxyPort"))));
		}
		return builder.build();
	}

	public static InputStream download(ModVersion v) {
		try(CloseableHttpResponse response = connectInOrder(buildURI(v.getDownloadUrl() + "/file").toURL().toString())){
			if(response == null){
				throw new IOException(
					String.format(
						"Couldn't connect to the web service at :%s",
						buildURI(v.getDownloadUrl() + "/file").toURL().toString()
					)
				);
			}

			try { return response.getEntity().getContent(); }
			catch (IOException e) { return null; }

		} catch (IOException | URISyntaxException e) {
			AppLogger.error(e.getMessage(), ModChecker.class);
			return null;
		}
	}

	//You're using firefox and THAT'S THAT
	private static String getUserAgentForOs() {
		String base = "Mozilla/5.0 (%s rv:65.0) Gecko/20100101 Firefox/65.0";
		String os = System.getProperty("os.name");
		if (os.matches(".*[Ww]indows.*"))
			return String.format(base, "Windows NT 10.0; Win64; x64;");
		else if (os.matches(".*[Mm]ac [Oo][Ss].*"))
			return String.format(base, "Macintosh; Intel Mac OS X 10.13;");
		else return String.format(base, "X11; Ubuntu; Linux x86_64;");
	}
}
