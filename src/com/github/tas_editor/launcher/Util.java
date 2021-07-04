package com.github.tas_editor.launcher;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Util {
	
	public static File getOwnFile() {
		return new File(URLDecoder.decode(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8));
	}

}
