# TAS-Editor-Launcher
A standalone launcher/updater for MonsterDruide1's TAS-Editor.

## Installation
To install the Launcher and the main Editor respectively, just go to the [latest release](https://github.com/MonsterDruide1/TAS-Editor-Launcher/releases/latest) and download it's `jar` file. On execution, it will create a new directory called `TAS-Editor` and create all required files in that directory. When done, it opens the Windows Explorer in that new folder, with the message to restart the launcher using the `bat` file.

On second execution, it will clean up all installation files, then self-update, followed by downloading the latest TAS-Editor-release. After that, the main Editor will be opened.

## Usage
For future reference, always start the launcher using the `bat` file. Manually starting its jar file will throw an error, and starting the TAS-Editor itself directly will disable the check for updates.
