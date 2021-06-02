package com.github.tas_editor.launcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class Launcher {

	public static void main(String[] args) {
		File updaterScript = new File("Launcher-updater.bat");
		if(updaterScript.exists()) updaterScript.delete(); //clean up file from self-update
		checkSelfUpdate(); //FIXME absolutely not tested yet
		Launcher launcher = new Launcher(new GithubAPI("MonsterDruide1", "TAS-editor"), Preferences.userRoot().node(Launcher.class.getName()));
		launcher.update();
		launcher.launch();
	}
	
	private static void checkSelfUpdate() {
		GithubAPI api = new GithubAPI("MonsterDruide1", "TAS-Editor-Launcher");
		try {
			JSONObject latest = api.getLatestRelease();
			int id = latest.getInt("id");
			if(id != Preferences.userRoot().node(Launcher.class.getName()).getInt("launcherID", 0)) {
				selfUpdate(latest.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"));
			}
		} catch (JSONException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private static void selfUpdate(String fileURL) throws MalformedURLException, IOException, URISyntaxException {
		downloadUpdate(fileURL, new File("Launcher-update.jar"));
		String ownFile = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		PrintWriter writer = new PrintWriter(new File("Launcher-updater.bat"));
		writer.write("taskkill /F /PID "+ProcessHandle.current().pid()+"\n"); //kill this process to modify the jar file
		writer.write("move Launcher-update.jar "+ownFile+"\n"); //replace this jar file
		writer.write("java -jar "+ownFile); //start the file up again
		writer.flush();
		writer.close();
		ProcessBuilder builder = new ProcessBuilder("cmd", "/C", "Launcher-updater.bat");
		builder.start();
		System.exit(0); //force exit if taskkill didn't do that yet
	}
	
	private GithubAPI api;
	private Preferences prefs;
	
	public Launcher(GithubAPI api, Preferences prefs) {
		this.api = api;
		this.prefs = prefs;
	}
	
	public void update() {
		try {
			JSONObject latestRelease = api.getLatestRelease();
			int localID = prefs.getInt("latestID", 0);
			if(localID != latestRelease.getInt("id")) {
				String changelog = generateChangelog(localID);
				displayChangelog(changelog);
				downloadUpdate(latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"), getEditorFile());
				prefs.putInt("latestID", latestRelease.getInt("id"));
			}
			
			System.out.println("Up to date!");
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void displayChangelog(String changelog) { //TODO improve layout
		JFrame frame = new JFrame();
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.insets = new Insets(10, 10, 10, 10);
		
		JLabel crash = new JLabel("Update available! Changelog:");
		crash.setFont(crash.getFont().deriveFont(25f));
		panel.add(crash, c);
		
		c.weighty = 1;
		c.gridy = 1;
		JTextArea area = new JTextArea(changelog);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		JScrollPane pane = new JScrollPane(area);
		pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(pane, c);
		
		frame.add(panel);
		frame.setTitle("Changelog");
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public String generateChangelog(int startID) throws JSONException, IOException {
		if(startID == 0) //first start -> download current release without changelog
			return "";
		
		ArrayList<String> changelogs = new ArrayList<>();
		int id;
		int page = 1;
		do {
			JSONObject release = api.getReleases(page++, 1).getJSONObject(0);
			changelogs.add(release.getString("body"));
			id = release.getInt("id");
		} while(id != startID);
		
		//changelogs are now ordered new-to-old, meaning the most actual changelogs are in index 0
		
		ArrayList<ChangelogEntry> changelogEntries = new ArrayList<>();
		for(String changelog : changelogs) {
			for(String line : changelog.split("[\r\n]+")) {
				changelogEntries.add(new ChangelogEntry(line));
			}
		}
		changelogEntries.sort(ChangelogEntry::compare);
		
		return changelogEntries.stream().map(ChangelogEntry::toString).collect(Collectors.joining("\r\n"));
	}
	
	public void launch() {
		try {
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", "java -jar "+getEditorFile().toString());
			builder.directory(new File(System.getProperty("user.dir")));
			builder.redirectErrorStream(true);
			builder.redirectOutput(getLogFile());
			Process process = builder.start();
			if(process.waitFor() != 0) {
 				showCrashLog();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void showCrashLog() throws IOException {
		String log = Files.readString(getLogFile().toPath());
		
		JFrame frame = new JFrame();
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.insets = new Insets(10, 10, 10, 10);
		
		JLabel crash = new JLabel("Uh oh, looks like TAS-Editor crashed...");
		crash.setFont(crash.getFont().deriveFont(25f));
		panel.add(crash, c);
		
		c.gridy = 1;
		JLabel submitLog = new JLabel("<html>Please submit the following log file to Jadefalke2 or MonsterDruide1 on the SMO-TAS Discord!<br/>"
				+ "It can also be found at "+getLogFile().toString()+".</html>");
		panel.add(submitLog, c);
		
		c.weighty = 1;
		c.gridy = 2;
		JTextArea area = new JTextArea(log);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		JScrollPane pane = new JScrollPane(area);
		pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(pane, c);
		
		frame.add(panel);
		frame.setTitle("TAS-Editor crashed!");
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static void downloadUpdate(String url, File toFile) throws MalformedURLException, IOException {
		Files.copy(new URL(url).openStream(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING); //TODO show progress
	}
	
	public File getEditorFile() {
		return new File(prefs.get("EditorPath", "TAS-Editor.jar"));
	}
	public File getLogFile() {
		return new File(prefs.get("logfile", "log.txt"));
	}

}