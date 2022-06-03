package com.github.tas_editor.launcher;

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

import org.json.JSONException;
import org.json.JSONObject;

public class Launcher {

	public static final String batFileID = "v1";
	private static final GithubAPI launcherAPI = new GithubAPI("MonsterDruide1", "TAS-Editor-Launcher");
	private static final GithubAPI editorAPI = new GithubAPI("MonsterDruide1", "TAS-Editor");

	// TODO custom implementation of Preferences?
	private static Preferences prefs;
	private static File preferencesFile;

	public static void main(String[] args) throws FileNotFoundException, IOException, InvalidPreferencesFormatException,
			BackingStoreException, URISyntaxException, InterruptedException {
		preferencesFile = new File("config/launcher.xml").getAbsoluteFile();
		if (preferencesFile.exists())
			Preferences.importPreferences(new FileInputStream(preferencesFile));
		else
			Preferences.userRoot().node(Launcher.class.getName()).clear();

		prefs = Preferences.userRoot().node(Launcher.class.getName());
		if (args.length == 0) { // first start or just didn't start using the bat
			File ownFile = Util.getOwnFile();
			System.out.println("own: "+ownFile);
			if (!prefs.getBoolean("installed", false) && !prefs.getBoolean("justInstalled", false)
					&& !ownFile.getParentFile().getName().equals("bin")) { // first start -> install
				System.out.println("First start! Creating file structure...");
				File installDir = ownFile.getParentFile();
				System.out.println("install: "+installDir);
				if (installDir.list().length != 1) { // not in its own directory
					installDir = new File(installDir + "/TAS-Editor");
					installDir.mkdirs();
				}
				new File(installDir + "/bin").mkdirs();
				Files.copy(ownFile.toPath(), Paths.get(installDir + "/bin/Launcher.jar"),
						StandardCopyOption.REPLACE_EXISTING);
				new File(installDir + "/log").mkdirs();
				writeLauncherBat(new File(installDir + "/Launcher.bat"));
				prefs.putBoolean("justInstalled", true);
				new File(installDir + "/config").mkdirs();
				preferencesFile = new File(installDir + "/config/launcher.xml");
				preferencesFile.createNewFile();
				Runtime.getRuntime().exec("explorer.exe \"" + installDir.toString() + "\"");
				prefs.exportSubtree(new FileOutputStream(preferencesFile));
				System.out.println("Done installing!");
			}
			UI.showMessageDialog("Please restart the launcher using the bat file!", "Restart using bat");
			System.exit(0);
			return;
		}

		if (prefs.getBoolean("justInstalled", false)) { // clean up original file
			System.out.println("Second start. Clean up installation files...");
			File possibleLocation1 = new File("Launcher.jar");
			File possibleLocation2 = new File("../Launcher.jar");
			if (possibleLocation1.exists())
				possibleLocation1.delete();
			else if (possibleLocation2.exists())
				possibleLocation2.delete();
			else
				UI.showMessageDialog(
						"Could not locate original installation file. As the Launcher is fully installed, it is not required anymore, please delete it.",
						"Could not delete original file");

			prefs.remove("justInstalled");
			prefs.putBoolean("installed", true);
			prefs.exportSubtree(new FileOutputStream(preferencesFile));
			System.out.println("Done cleaning up!");
		}

		if (!args[0].equals(batFileID)) { // is used if the bat should be updated
			System.out.println("Update the bat file...");
			writeLauncherBat(new File("Launcher.bat"));
			UI.showMessageDialog("Please restart the launcher using the bat file!", "Restart using bat");
			System.exit(0);
			return;
		}
		System.err.println("PLEASE DO NOT CLOSE THIS WINDOW - it is required to detect crashes of the TAS-Editor!");
		System.err.println("-------------------------------------------------------------------------------------");

		File updaterScript = new File("bin/Launcher-updater.bat");
		if (updaterScript.exists())
			updaterScript.delete(); // clean up file from self-update

		checkSelfUpdate();
		update();
		prefs.exportSubtree(new FileOutputStream(preferencesFile));
		launch();
	}

	private static void writeLauncherBat(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		pw.write("@ECHO OFF\n");
		pw.write("start \"TAS-Editor-Launcher - DO NOT CLOSE THIS WINDOW\" /MIN cmd /c \"java -jar bin/Launcher.jar "
				+ batFileID + " & if ERRORLEVEL 3 call bin/Launcher-updater.bat\"");
		pw.flush();
		pw.close();
	}

	private static void checkSelfUpdate() throws JSONException, IOException, URISyntaxException, BackingStoreException {
		JSONObject latest = launcherAPI.getLatestRelease();
		int id = latest.getInt("id");
		if (id != Preferences.userRoot().node(Launcher.class.getName()).getInt("launcherID", 0)) {
			System.out.println("Performing selfUpdate...");
			selfUpdate(latest.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"), id);
		}
	}

	private static void selfUpdate(String fileURL, int id)
			throws MalformedURLException, IOException, URISyntaxException, BackingStoreException {
		downloadUpdate(fileURL, new File("bin/Launcher-update.jar"));
		System.out.println("Done downloading Launcher-update file. Writing updater script...");
		File ownFileFile = Util.getOwnFile();
		String ownFile = ownFileFile.getName();
		PrintWriter writer = new PrintWriter(new File("bin/Launcher-updater.bat"));
		writer.write("move /Y \"" + ownFileFile.getParentFile().getAbsolutePath() + "\\Launcher-update.jar\" \""
				+ ownFileFile.getParentFile().getAbsolutePath() + "\\" + ownFile + "\"\n"); // replace this jar file
		writer.write("call Launcher.bat"); // start the file up again
		writer.flush();
		writer.close();
		prefs.putInt("launcherID", id);
		prefs.exportSubtree(new FileOutputStream(preferencesFile));
		System.out.println("Done. Restarting...");
		System.exit(3); // request update from underlying batch file
	}

	public static void update() throws MalformedURLException, JSONException, IOException {
		JSONObject latestRelease = editorAPI.getLatestRelease();
		int localID = prefs.getInt("latestID", 0);
		if (localID != latestRelease.getInt("id") || !getEditorFile().exists()) {
			System.out.println("Update for TAS-Editor available! Checking validity...");
			if (latestRelease.has("assets") && latestRelease.getJSONArray("assets").length() > 0
					&& latestRelease.getJSONArray("assets").getJSONObject(0).has("browser_download_url")) {
				if (localID != 0) {
					String changelog = generateChangelog(localID);
					UI.displayChangelog(changelog);
				}
				System.out.println("Downloading update...");
				downloadUpdate(latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"),
						getEditorFile());
				prefs.putInt("latestID", latestRelease.getInt("id"));
			}
		}

		System.out.println("Up to date!");
	}

	public static String generateChangelog(int startID) throws JSONException, IOException {
		if (startID == 0) // first start -> download current release without changelog
			return "";

		ArrayList<String> changelogs = new ArrayList<>();
		int id;
		int page = 1;
		do {
			JSONObject release = editorAPI.getReleases(page++, 1).getJSONObject(0);
			changelogs.add(release.getString("body"));
			id = release.getInt("id");
		} while (id != startID);
		changelogs.remove(changelogs.size() - 1); // remove last, as it's the changelog for the already-installed
													// version

		// changelogs are now ordered new-to-old, meaning the most actual changelogs are
		// in index 0

		ArrayList<ChangelogEntry> changelogEntries = new ArrayList<>();
		for (String changelog : changelogs) {
			for (String line : changelog.split("[\r\n]+")) {
				changelogEntries.add(new ChangelogEntry(line));
			}
		}
		changelogEntries.sort(ChangelogEntry::compare);

		return changelogEntries.stream().map(ChangelogEntry::toString).collect(Collectors.joining("\r\n"));
	}

	public static void launch() throws IOException, InterruptedException {
		System.out.println("Launching TAS-Editor...");
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", "java -jar " + getEditorFile().toString());
		builder.redirectErrorStream(true);
		builder.redirectOutput(getLogFile());
		Process process = builder.start();
		System.out.println("Done! Waiting for crash/exit...");
		if (process.waitFor() != 0) {
			UI.showCrashLog(getLogFile());
		}
		System.out.println("Exiting...");
	}

	public static void downloadUpdate(String url, File toFile) throws MalformedURLException, IOException {
		Files.copy(new URL(url).openStream(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING); // TODO show
																										// progress
	}

	public static File getEditorFile() {
		return new File(prefs.get("EditorPath", "bin/TAS-Editor.jar"));
	}

	public static File getLogFile() {
		return new File(prefs.get("logfile", "log/log.txt"));
	}

}
