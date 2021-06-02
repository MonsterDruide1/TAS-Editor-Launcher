package com.github.tas_editor.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GithubAPI {
	
	public static final String GITHUB_API = "https://api.github.com/";
	
	
	private String owner, repo;
	
	public GithubAPI(String owner, String repo) {
		this.owner = owner;
		this.repo = repo;
	}
	
	public JSONObject getLatestRelease() throws JSONException, IOException {
		String url = GITHUB_API+"repos/"+owner+"/"+repo+"/releases/latest";
		return getJSONObject(url);
	}

	public JSONArray getReleases(int page, int itemsPerPage) throws JSONException, IOException {
		String url = GITHUB_API+"repos/"+owner+"/"+repo+"/releases?page="+page+"&per_page="+itemsPerPage;
		return getJSONArray(url);
	}
	
	
	
	
	

	private JSONArray getJSONArray(String url) throws JSONException, IOException {
		return new JSONArray(getString(url));
	}
	private JSONObject getJSONObject(String url) throws JSONException, IOException {
		return new JSONObject(getString(url));
	}
	
	private String getString(String urlString) throws IOException {
		URL url = new URL(urlString);
		try(InputStream in = url.openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}
