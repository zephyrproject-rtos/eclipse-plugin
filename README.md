# Eclipse Plug-in for Zephyr Project

## Overview

This contains a set of Eclipse plug-ins which extends Eclipse CDT to support
application development on Zephyr RTOS, including building and target hardware
debugging.

## How to Install

#### Prerequisites

The minimum requirements for running the plugins are:

* Java SE 8 Update 212 or later
* Eclipse Neon.3 (4.6.3) with CDT 9.2.1

Also, please make sure the development host is setup to build Zephyr
application by following the [Getting Started](https://docs.zephyrproject.org/latest/getting_started/index.html)
guide.

#### Installing the Plugin

The plugin can be installed via update sites in Eclipse.

1. Go to `Help` in the menu bar, and select `Install New Software`.
2. Click the `Add` button to add a new update site. Choose one of
   the following sites:
   * For stable releases:
     * Name: `zephyr-eclipse-stable`
     * URL: `TBD`
   * For experimental releases:
     * Name: `zephyr-eclipse-experimental`
     * URL: `TBD`
3. Select the newly added update site.
4. Select `Zephyr Project` and `Zephyr Project Development Support` and
   click `Next`.
5. Follow the instructions on the wizards to install the plugin.
6. Restart Eclipse when asked to do so.

#### Updating the Plugin

Go to `Help` in the menu bar, and select `Check for Updates`.

## How to Use

#### Create a New Project for Zephyr Application

Project creation is similar to creating other project type under Eclipse.
This will create a `Hello World` project.

1. Go to `File`, `New` and `Project...`
2. In the dialog, select `Zephyr Application` under `Zephyr Project`.
3. Click `Next`
4. Type in the name for the project.
5. Under `Zephyr Base Path (ZEPHYR_BASE)`, put in (or browse to) the path
   to the Zephyr tree.
6. Click `Next`
7. Select the toolchain variant to be used with this project. Depending on
   the selection, different set of options will need to be set. These options
   are the same one as described in the
   [Getting Started - Set Up a Toolchain](https://docs.zephyrproject.org/latest/getting_started/index.html#set-up-a-toolchain).
8. Click `Next`
9. Type in the board name of the targat hardware.
   * Or click on the check box and select one below.
10. Click `Finish`
11. A new project is created under Eclipse.
12. It is recommended to use the `C/C++` perspective.
    1. Go to `Window`, `Perspective`, `Open Perspective`, and `Other...`.
    2. Select `C/C++` and click `Ok`.

#### Building the Project

* Right click on the project name under `Project Explorer` and select
  `Build Project`.

#### Run Emulator

1. Right click on the project name under `Project Explorer` and select
   `Run As`, and `Run Configurations...`.
2. Right click on `Zephyr Emulator Target`, and `New`.
   * Or, select a previously created configuration.
3. In the newly created configuration, make sure the project is the correct
   one.
4. Click `Run`
5. The `Console` view will contain the output of the emulator.
6. Click the red square `Terminate` to stop the emulator.

#### Flash to Hardware

1. Right click on the project name under `Project Explorer` and select
   `Run As`, and `Run Configurations...`.
2. Right click on `Zephyr Hardware Target`, and `New`.
   * Or, select a previously created configuration.
3. In the newly created configuration, make sure the project is the correct
   one.
4. Click `Run`
5. The `Console` view will contain the output of the flashing process.

#### Debugging on Emulator

1. Right click on the project name under `Project Explorer` and select
   `Debug As`, and `Debug Configurations...`.
2. Right click on `Zephyr Emulator Target`, and `New`.
   * Or, select a previously created configuration.
3. In the newly created configuration, make sure the project is the correct
   one, and the `C/C++ Application` points to the correct ELF file.
4. In the `Debugger` tab:
   * The `GDB Command` should be pre-populated with the GDB project
     discovered by CMake. If not, select the correct GDB program corresponding
     to the target.
   * Port number is pre-populated according to the target type.
     Change if it is not correct.
5. Click `Debug` and the debug session should start.
6. Follow Eclipse's debugging workflow to debug the application.

#### Debugging on Hardware

1. Right click on the project name under `Project Explorer` and select
   `Debug As`, and `Debug Configurations...`.
2. Right click on `Zephyr Hardware Target`, and `New`.
   * Or, select a previously created configuration.
3. In the newly created configuration, make sure the project is the correct
   one, and the `C/C++ Application` points to the correct ELF file.
4. In the `Main` tab, default is to also flash the image to the target.
   This can be skipped with the appropriate option.
5. In the `Debugger` tab:
   * The `GDB Command` should be pre-populated with the GDB project
     discovered by CMake. If not, select the correct GDB program corresponding
     to the target.
   * Port number is pre-populated according to the target type.
     Change if it is not correct.
6. Click `Debug` and the debug session should start.
7. Follow Eclipse's debugging workflow to debug the application.
