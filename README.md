<a name="sec-about"> </a>
About n-Puzzle
==============

*n-Puzzle* app is a classic sliding puzzle game based on the
[Fifteen Puzzle](http://en.wikipedia.org/wiki/15_puzzle). It allows you
to choose among three levels of complexity: 3x3 (8-puzzle), 4x4 (15-puzzle),
and 5x5 (24-puzzle), hence the name.

In addition to standard square boards with numbers, you can choose a board
with a picture painted on it. There are three stock pictures installed with the
puzzle, and you can add your own pictures by pressing `Menu` button when the
picture selection page is displayed. The picture you select will be split into
equal tiles.

Once you select the picture, the solved puzzle is displayed for 3 seconds.
Then it is randomly shuffled, and you have to move the tiles to their initial
locations. When you solve the puzzle, the app will display that picture again
along with the number of moves you have made.

To change the puzzle's settings during the game, press `Menu` button. Note that
when you change any settings, the game starts over. When you switch apps or
exit during the game, your puzzle is stored and resumed next time you run
*n-Puzzle*.

<a name="sec-repo"> </a>
About this repository
=====================

This repository contains the source code of the *__n-Puzzle__ game* for
Android devices. Its top-level components are:

        src/           		n-Puzzle source files
        res/           		Structured data that doesn't change once
        					 n-Puzzle is installed
        assets/           	Raw data files that don't change once
        					 n-Puzzle is installed
        LICENSE/           	Documents that describe the project's licensing terms
        NOTICE.html         A summary of licenses that apply to n-Puzzle with
                       		 references to detailed legal documents
        build.xml      		Configuration file for the tool (Ant) that builds the
                       		 n-Puzzle's application package
        .classpath     		Eclipse configuration file for the project
        .project       		Eclipse configuration file for the project
        AndroidManifest.xml	Android application manifest
        project.properties	Android application configuration file
        proguard.cfg		Android application configuration file
        README.md			This document

<a name="android-sdk"> </a>
> **Note:** instructions in this file were written and tested with OpenJDK 6
> and [Replicant SDK 4.0](http://replicant.us/category/sdk/) on Linux. The
> build process _should_ be compatible with
> [Android SDK](http://developer.android.com/sdk/)
> for API level 8 or greater. However, you may need to tweak the build process
> to accommodate your SDK. See
> [Android application building instructions][android-build] for
> introduction into the Android application build process.

<a name="sec-building"> </a>
Building n-Puzzle
=================

To build an application package from this repository, you need:

   - [Replicant SDK 4.0](http://replicant.us/2013/01/replicant-4-0-sdk-release/)
   or a compatible **Android SDK** (see the [above note](#android-sdk)).

   - A **Java SDK**, also known as JDK, Standard Edition (SE), version 5 or
   later, available from OpenJDK <http://openjdk.java.net/> or Oracle
   <http://www.oracle.com/technetwork/java/javase/downloads/index.html>.
   The Java SDK must be compatible with Replicant or Android SDK that you
   use, see the SDK's file `docs/sdk/requirements.html` for specifics.

   Even though a Java runtime may already be installed on your machine
   (check that by running `java --version`), the build will fail if you
   don't have a complete JDK (check that by running `javac`).

   - **Apache Ant** version 1.8.0 or newer, available from the Apache Software
   Foundation <http://ant.apache.org/>.

   - _(optional) **Eclipse 3.6** (Helios)_ or greater. Though you don't need it
   to build the project, Eclipse will make it easier for you to read or write
   code if you want to contribute. If you are going to use
   [Android Development Tools (ADT) plugin][adt-plugin] with Eclipse
   (recommended), you should check the system requirements of that plugin as
   it may need a more recent version of Eclipse.

   - _(optional)_ A **key pair** to sign the distribution with, if you want
   to publish an application package built from this code. Note that you must
   comply with [n-Puzzle's license][lic] whenever you publish or otherwise
   distribute it.

We further assume that you have properly configured `JAVA_HOME` and `ANT_HOME`
environment variables and that you can run `ant`, `java`, and the Android tools
without prefixes on the command line. You may want to tweak the following
commands if you have a different configuration.

<a name="sec-import"> </a>
Importing the project from this repository
------------------------------------------

Once you have a [working copy][git-working-copy] of this repository, you need
to create some configuration files there that describe your local environment
to the SDK. Normally, you do this the first time you clone this project onto
your machine.

To create the configuration files for Android or Replicant SDK, replace
`{working-copy-location}` with the directory that holds the project's working
copy and run:

     android update project --name n-Puzzle --path {working-copy-location}

<a name="sec-build-debug"> </a>
Building for private use
------------------------

Before you build *n-Puzzle* from a working copy of this repository, make sure
you have configured your copy by
[importing the project with your SDK](#sec-import).

To build *n-Puzzle* for private use, chdir to your working copy location and
run:

     ant debug

The result is a debug mode `n-Puzzle-debug.apk` file inside the `bin`
subdirectory. You can install that file on an emulator or upload it to an
Android device.

> *Note:* since the Android debug key is different from the key used to sign
> the binary distribution of *n-Puzzle*, you will not be able to
> install the private build on a device that has the downloaded app installed. 
> To install both apps on one device, you'll have to change the package names
> on the project's source files.

<a name="sec-build-public"> </a>
Building for public distribution
--------------------------------

When building *n-Puzzle* for public distribution, you must sign it with a
key that identifies you the distributor to users that will install the
application. You must also comply with the [n-Puzzle's license][lic]
whenever you distribute *n-Puzzle*.

Before you build *n-Puzzle* from a working copy of this repository, make sure
you have configured your copy by
[importing the project with your SDK](#sec-import).

One possible process of building a public distribution of *n-Puzzle* consists
of the following steps:

1. Obtain or create a private key for signing.

   For help with creating your Android application signing keys, please refer
   to the [signing guide online][signing] or the
   `docs/guide/publishing/app-signing.html` page of your SDK. The key pairs
   are generated with [keytool][java-sectools] from the Java SDK.

2. Compile *n-Puzzle* for public release by running the command below in the
   directory that holds your working copy:

        ant release

   The result is an unsigned APK file `bin/n-Puzzle-release-unsigned.apk`.

3. Sign the new APK file using the [jarsigner][java-sectools] tool. You will
have to provide the following arguments:
<table style="margin: 1em 0 1em 1em; border: solid 1pt; border-spacing: 0">
<tr><th style="border: solid 1pt;">Placeholder</th>
<th style="border: solid 1pt;">Value</th></tr>
<tr><td style="border: solid 1pt;"><code>{key-password}</code></td>
<td style="border: solid 1pt;">The signing key's password</td></tr>
<tr><td style="border: solid 1pt;"><code>{store-password}</code></td>
<td style="border: solid 1pt;">The keystore password</td></tr>
<tr><td style="border: solid 1pt;"><code>{keystore-location}</code></td>
<td style="border: solid 1pt;">The location of your keystore,
usually a file name</td></tr>
<tr><td style="border: solid 1pt;"><code>{key-alias}</code></td>
<td style="border: solid 1pt;">The short name that identifies the
signing key within its keystore</td></tr>
</table>

        jarsigner -verbose -keypass {key-password} -storepass {store-password} \
         -signedjar bin/n-Puzzle-release-unaligned.apk \
         -keystore {keystore-location} bin/n-Puzzle-release-unsigned.apk puzzle
         {key-alias}

   The above command line has been tested with OpenJDK 6. With other JDK versions,
   the syntax may differ.

4. Align the signed package using the [zipalign][] tool.

        zipalign -f -v 4 bin/n-Puzzle-release-unaligned.apk \
         bin/n-Puzzle-release.apk

   The resulting application package `bin/n-Puzzle-release.apk` can be
   installed on devices and distributed online subject to the
   [license terms][lic].

> *Note:* since the key you will be signing your distribution with is different
> from the key used to sign the official binary distribution of *n-Puzzle*, you
> will not be able to install the APK you have built on a device that has that
> binary installed. To install both apps on one device, you'll have to change
> the package names on the project's source files.

<a name="sec-install"> </a>
Installing on a device
======================

There are multiple methods of installing *n-Puzzle* on an Android or Replicant
device. Whatever method you choose, make sure that your device has the
**Unknown sources** application setting enabled before installing *n-Puzzle*.

If the application was [built for private use](#sec-build-debug) (in the debug
mode), you can either:

 - Install *n-Puzzle* using the [`adb` tool][adb] from Android or Replicant SDK,
 - Run it on a device using [ADT plugin for Eclipse][adt-plugin], which installs
   the app for you, or
 - Upload the APK file via email or other means and open it on your device.

Note that you may have to enable USB debugging on your device before you can
deploy the application to it using [adb][].

If the application was [built for publishing](#sec-build-public) (in the release
mode), you can:

 - Publish the APK on a web site, then download and open it on your device,
 - Upload the APK file via email or other means and open it on your device, or
 - Install *n-Puzzle* using the [`adb` tool][adb] from Android or Replicant SDK.

<a name="sec-hack"> </a>
Hacking *n-Puzzle*
==================

Once you have a clone of this repository, you can import it into Eclipse using
the [Android Development Tools (ADT) plugin][adt-plugin]. Make sure that your
plugin is configured with the Android or Replicant SDK location (under
`Window > Preferences > Android`) and that the list of SDK targets on that
preferences page contains at least one target with API level 8 or greater.
Before proceeding with these instructions, make sure you have
[imported](#sec-import) your working copy of the project into SDK as explained
[above](#sec-import).

Select `File > Import...`  from the Eclipse menu and choose
`General > Existing Projects into Workspace` as the item to import. Select
the directory that contains your working copy of this repository, check the
checkbox against `n-Puzzle` item on the list, and uncheck the `Copy projects
into workspace` checkbox, then click `Finish`.

Now you have a project in Eclipse that gives you access to all the application's
code.

<a name="sec-contact"> </a>
Contacting the project's team
=============================

You can send a message to the project's team via the
[Contact page](http://www.livitski.com/contact) at <http://www.livitski.com/>
or via *GitHub*. We will be glad to hear from you!

 [lic]: NOTICE.html "License notice"
 [git-working-copy]: http://git-scm.com/book/en/Git-Basics-Getting-a-Git-Repository#Cloning-an-Existing-Repository "What's the Git working copy"
 [signing]: http://developer.android.com/tools/publishing/app-signing.html "Aigning an Android application"
 [java-sectools]: http://docs.oracle.com/javase/6/docs/technotes/tools/index.html#security "Java SDK security tools"
 [zipalign]: http://developer.android.com/tools/help/zipalign.html "zipalign tool"
 [adt-plugin]: http://developer.android.com/tools/help/adt.html "ADT plug-in for Eclipse"
 [android-build]: http://developer.android.com/tools/building/building-cmdline.html "Building an Android app from the command line"
 [adb]: http://developer.android.com/tools/help/adb.html "Android Debug Bridge tool"