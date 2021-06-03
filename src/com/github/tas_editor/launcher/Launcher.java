package com.github.tas_editor.launcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class Launcher {

	public static final String batFileID = "v1";
	
	private static Preferences prefs;

	public static void main(String[] args) throws FileNotFoundException, IOException, InvalidPreferencesFormatException, BackingStoreException {
		File preferencesFile = new File("config/launcher.xml").getAbsoluteFile();
		if(preferencesFile.exists())
			Preferences.importPreferences(new FileInputStream(preferencesFile));
		else
			Preferences.userRoot().node(Launcher.class.getName()).clear();
		
		prefs = Preferences.userRoot().node(Launcher.class.getName());
		if(args.length == 0) { //first start or just didn't start using the bat
			if(!prefs.getBoolean("installed", false) && !prefs.getBoolean("justInstalled", false)) { //first start -> install
				System.out.println("First start! Creating file structure...");
				try {
					File ownFile = new File(
							Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					File installDir = ownFile.getParentFile();
					if(installDir.list().length != 1) { //not in its own directory
						installDir = new File(installDir+"/TAS-Editor");
						installDir.mkdirs();
					}
					new File(installDir+"/bin").mkdirs();
					Files.copy(ownFile.toPath(), Paths.get(installDir+"/bin/Launcher.jar"), StandardCopyOption.REPLACE_EXISTING);
					new File(installDir+"/log").mkdirs();
					writeLauncherBat(new File(installDir+"/Launcher.bat"));
					prefs.putBoolean("justInstalled", true);
					new File(installDir+"/config").mkdirs();
					preferencesFile = new File(installDir+"/config/launcher.xml");
					preferencesFile.createNewFile();
					prefs.exportSubtree(new FileOutputStream(preferencesFile));
				} catch (URISyntaxException | IOException e) {
					e.printStackTrace();
				}
				System.out.println("Done installing!");
			}
			showMessageDialog("Please restart the launcher using the bat file!", "Restart using bat");
			System.exit(0);
			return;
		}
		
		if(prefs.getBoolean("justInstalled", false)) { //clean up original file
			System.out.println("Second start. Clean up installation files...");
			File possibleLocation1 = new File("Launcher.jar");
			File possibleLocation2 = new File("../Launcher.jar");
			if(possibleLocation1.exists())
				possibleLocation1.delete();
			else if(possibleLocation2.exists())
				possibleLocation2.delete();
			else
				showMessageDialog("Could not locate original installation file. As the Launcher is fully installed, it is not required anymore, please delete it.", "Could not delete original file");
				
			prefs.remove("justInstalled");
			prefs.putBoolean("installed", true);
			prefs.exportSubtree(new FileOutputStream(preferencesFile));
			System.out.println("Done cleaning up!");
		}
		
		if (!args[0].equals(batFileID)) { // is used if the bat should be updated
			System.out.println("Update the bat file...");
			writeLauncherBat(new File("Launcher.bat"));
			showMessageDialog("Please restart the launcher using the bat file!", "Restart using bat");
			System.exit(0);
			return;
		}
		System.err.println("PLEASE DO NOT CLOSE THIS WINDOW - it is required to detect crashes of the TAS-Editor!");
		System.err.println("-------------------------------------------------------------------------------------");

		File updaterScript = new File("bin/Launcher-updater.bat");
		if (updaterScript.exists())
			updaterScript.delete(); // clean up file from self-update

		checkSelfUpdate(preferencesFile);
		Launcher launcher = new Launcher(new GithubAPI("MonsterDruide1", "TAS-editor"));
		launcher.update();
		prefs.exportSubtree(new FileOutputStream(preferencesFile));
		launcher.launch();
	}

	private static void showMessageDialog(String message, String title) {
		JFrame frame = new JFrame(title);

		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);

		frame.dispose();
	}

	private static void writeLauncherBat(File file) {
		try {
			PrintWriter pw = new PrintWriter(file);
			pw.write("@ECHO OFF\n");
			pw.write("start \"TAS-Editor-Launcher - DO NOT CLOSE THIS WINDOW\" /MIN cmd /c \"java -jar bin/Launcher.jar " + batFileID
					+ " & if ERRORLEVEL 3 call bin/Launcher-updater.bat\"");
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void checkSelfUpdate(File preferencesFile) {
		GithubAPI api = new GithubAPI("MonsterDruide1", "TAS-Editor-Launcher");
		try {
			JSONObject latest = api.getLatestRelease();
			int id = latest.getInt("id");
			if (id != Preferences.userRoot().node(Launcher.class.getName()).getInt("launcherID", 0)) {
				System.out.println("Performing selfUpdate...");
				selfUpdate(latest.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"), id, preferencesFile);
			}
		} catch (JSONException | IOException | URISyntaxException | BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private static void selfUpdate(String fileURL, int id, File preferencesFile) throws MalformedURLException, IOException, URISyntaxException, BackingStoreException {
		downloadUpdate(fileURL, new File("bin/Launcher-update.jar"));
		System.out.println("Done downloading Launcher-update file. Writing updater script...");
		File ownFileFile = new File(
				Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		String ownFile = ownFileFile.getName();
		PrintWriter writer = new PrintWriter(new File("bin/Launcher-updater.bat"));
		writer.write("taskkill /F /PID " + ProcessHandle.current().pid() + "\n"); // kill this process to modify the jar file
		writer.write("move /Y \""+ownFileFile.getParentFile().getAbsolutePath()+"\\Launcher-update.jar\" \""+ownFileFile.getParentFile().getAbsolutePath()+"\\" + ownFile + "\"\n"); // replace this jar file
		writer.write("call Launcher.bat"); // start the file up again
		writer.flush();
		writer.close();
		Preferences.userRoot().node(Launcher.class.getName()).putInt("launcherID", id);
		prefs.exportSubtree(new FileOutputStream(preferencesFile));
		System.out.println("Done. Restarting...");
		System.exit(3); // request update from underlying batch file
	}

	private GithubAPI api;

	public Launcher(GithubAPI api) {
		this.api = api;
	}

	public void update() {
		try {
			JSONObject latestRelease = api.getLatestRelease();
			int localID = prefs.getInt("latestID", 0);
			if (localID != latestRelease.getInt("id") || !getEditorFile().exists()) {
				System.out.println("Update for TAS-Editor available! Generating Changelog...");
				String changelog = generateChangelog(localID);
				displayChangelog(changelog);
				System.out.println("Downloading update...");
				downloadUpdate(latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"),
						getEditorFile());
				prefs.putInt("latestID", latestRelease.getInt("id"));
			}

			System.out.println("Up to date!");
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	public void displayChangelog(String changelog) { // TODO improve layout
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
		area.setEditable(false);
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
		if (startID == 0) // first start -> download current release without changelog
			return "";

		ArrayList<String> changelogs = new ArrayList<>();
		int id;
		int page = 1;
		do {
			JSONObject release = api.getReleases(page++, 1).getJSONObject(0);
			changelogs.add(release.getString("body"));
			id = release.getInt("id");
		} while (id != startID);
		changelogs.remove(changelogs.size()-1); //remove last, as it's the changelog for the already-installed version

		// changelogs are now ordered new-to-old, meaning the most actual changelogs are in index 0

		ArrayList<ChangelogEntry> changelogEntries = new ArrayList<>();
		for (String changelog : changelogs) {
			for (String line : changelog.split("[\r\n]+")) {
				changelogEntries.add(new ChangelogEntry(line));
			}
		}
		changelogEntries.sort(ChangelogEntry::compare);

		return changelogEntries.stream().map(ChangelogEntry::toString).collect(Collectors.joining("\r\n"));
	}

	public void launch() {
		try {
			System.out.println("Launching TAS-Editor...");
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", "java -jar " + getEditorFile().toString());
			builder.directory(new File(System.getProperty("user.dir")));
			builder.redirectErrorStream(true);
			builder.redirectOutput(getLogFile());
			Process process = builder.start();
			System.out.println("Done! Waiting for crash/exit...");
			if (process.waitFor() != 0) {
				showCrashLog();
			}
			System.out.println("Exiting...");
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
		JLabel submitLog = new JLabel(
				"<html>Please submit the following log file to Jadefalke2 or MonsterDruide1 on the SMO-TAS Discord!<br/>"
						+ "It can also be found at " + getLogFile().toString() + ".</html>");
		panel.add(submitLog, c);

		c.weighty = 1;
		c.gridy = 2;
		JTextArea area = new JTextArea(log);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
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
		Files.copy(new URL(url).openStream(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING); // TODO show progress
	}

	public File getEditorFile() {
		return new File(prefs.get("EditorPath", "bin/TAS-Editor.jar"));
	}

	public File getLogFile() {
		return new File(prefs.get("logfile", "log/log.txt"));
	}

}
