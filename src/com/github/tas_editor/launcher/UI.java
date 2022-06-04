package com.github.tas_editor.launcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class UI {

	public static void showMessageDialog(String message, String title) {
		JFrame frame = new JFrame(title);

		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);

		frame.dispose();
	}

	public static void displayChangelog(String changelog) { // TODO improve layout
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

	public static void showCrashLog(File logFile) throws IOException {
		String log = Files.readString(logFile.toPath());

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
				"<html>Please submit the following log file to MonsterDruide1 on the SMO-TAS Discord!<br/>"
						+ "It can also be found at " + logFile.toString() + ".</html>");
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
	
}
