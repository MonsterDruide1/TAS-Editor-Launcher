package com.github.tas_editor.launcher;

import java.io.File;

public class Util {
	
	public static File getOwnFile() {
		return new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
	}

}
