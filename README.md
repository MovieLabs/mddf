![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MovieLabs Digital Distribution Framework—MDDF

1 [What's New](#h_News)

2 [Overview](#h_Overview)

3 [Status](#h_Status)

4 [Software Organization & Roadmap](#h_Roadmap)

5 [Executable Packages](#h_Versions)

6 [Installing and Running](#h_Install)
  1. [Java](#h_Install_Java)
  1. [OS-X](#h_Install_OSX)
  1. [Windows](#h_Install_MS)
  1. [Linux](#h_Install_LINUX)

7 [Past Releases](#h_History)

---
## <a name="h_News">What's New</a>

* **mddf-tool v1.3.rc3 available for testing and evaluation.

## <a name="h_Overview">Overview:</a>

This repository contains Java software supporting the creation and usage of MDDF files including

* Avails
* Common Media Manifest (CMM)
* Media Manifest Core (MMC)
* Media Entertainment Core (MEC)

Information on the MovieLabs Digital Distribution Framework and the various MDDF standards is available at <http://www.movielabs.com/md/>

## <a name="h_Status">Status and Relationship to Other MovieLabs Github Repositories:</a>

The software in this repository is intended to replace that contained in the following MovieLabs repositories:

* availslib
* availstool
* cpe-preprocessing

## <a name="h_Roadmap">Software Organization & Roadmap:</a>

There are two projects within this repository:

* __mddf-lib__: this implements all core (i.e., *non-UI*) functionality that can be used to generate, validate, or transform MDDF files.
* __mddf-tools__: implements standalone applications for performing MDDF-related tasks.

The applications in mddf-tools are implemented on top of mddf-lib and any developers intending to develop their 
own mddf support applications are encouraged to do the same.

The mddf-tools software currently implements two applications:

   - **Avails Validator Tool**: may be used to
      - validate an Avails file specified as either XML or XLSX
      - translate an XLSX-formatted Avails to the equivalent XML
      - translate an XML-formatted Avails to the equivalent XLSX
   
   - **Manifest Validator**: validates that a CMM, MMC, or MEC file conforms with:
     - the CMM schema
     - recommended 'Best Practices'
     - a specific *profile*

## <a name="h_Versions">Executables</a>

### <a name="h_formats">Available Formats:</a>

Binary releases are available in two forms:

* as Native Executables for Windows, OS-X, and Linux systems. Each executable is specific to the processing of either Avails or Manifest files. These may be downloaded from the MovieLabs web site:

   * Manifest Validator: <http://movielabs.com/md/manifest/validator/>
   * Avails Validator: <http://movielabs.com/md/avails/validator/>

* as an executable Java jar which supports the processing of either Avails or Manifest files. Jar files may be downloaded from the ./binaries directory of this repository. [NOTE: Version prior to v1.1.3 were not released as executable jars.]

Note that the executable jar provides all of the capabilities available via the two native excutable packages. In addition, the jar provides a command
line interface (CLI) that may be used either from a terminal window or in conjunction with scheduled jobs (e.g., via crontab).

Refer to ChangeLog.md for a list of specific enhancements and bug fixes for any given release.

### Versioning:
#### mddf-lib and Java jars:
Stable releases of the mddf-lib will have a two-part version or a three-part version ID. 
Interim development and evaluation (i.e., BETA) releases will include an "rcN" suffix. 

Examples:
* v1.3 or v2.0 identify stable releases
* v2.0.1.rc2 is a Beta release of v2.0.1

Executable jars are created for both stable and BETA releases and track the version number of the underlying mddf-lib.

Example: mddf-tool-v3.1.jar would based on, and compatible with, v3.1 of mddf-lib.

#### Native executables:
Beginning with the release of v1.3 of mddf-lib, native executables are assigned versions that append a letter to the 
the underlying mddf-lib version.

Examples: 
* an Avails Validator with a version of v1.3.a is based on v1.3 of the mddf-lib.
* a Manifest Validator with a version of v1.3.2.c is based on v1.3.2 of the mddf-lib.

Prior to July 2017 and the release of v1.3 of the mddf-lib, the native executables were assigned a version ID
independent of the mddf-lib version. 

## <a name="h_Install">Installing and Running:</a>

The use of the Java executable Jar is highly recommended as it is always the most up-to-date version and provides 
the full range of capabilities. Native executables for OS-X, Windows, and Linux are also provided but will
not be available for all developmental candidate releases. See  [Available Formats](#h_formats) for download 
locations for all versions.

### <a name="h_Install_Java">Java Executable Jar:</a>

Once the jar file has been downloaded, it may be run using the standard Java command:

`java -jar ./{path-to-directory}/mddf-tool-v{version}.jar -i`

The `-i` argument indicates the toolkit should be started in interactive mode. A 
`Tool Launcher`dialog will appear that allows the user to select the MDDF tool they 
wish to use (e.g., the Avails Validator). To see all supported arguments, use `-h`

### <a name="h_Install_OSX">OS-X:</a>

Due to issues with Apple's Gatekeeper security mechanism and JavaFX applications, OS-X executables are provided 
in the form of zip files. Unlike with the Java jars, a separate file must be
downloaded and installed for each MDDF tool.

1. unzip the downloaded file(s) to the desired location (e.g. `/Applications`)
1. make the applications executable. Open a terminal and enter the following:
```
  cd {install-directory}
  chmod -R a+x ./AvailsValidator.app
  chmod -R a+x ./ManifestValidator.app
```

The application may now be launched via the Finder. An error message that “the application can’t be opened” 
indicates Step #2 was not performed correctly.

### <a name="h_Install_MS">Windows:</a>

Executables for Windows environments are provided in the form of standard `.exe` files and require no special
steps to install or run. The `.msi` formatted distro is not supported.

### <a name="h_Install_LINUX">Linux:</a>

Linux executables are provided in both`rpm` and `deb` distro formats.

## <a name="h_History">Release History:</a>

* **2017-Sep-21**: mddf-tool v1.3.rc3 released for testing and evaluation.

* **2017-May-09**: mddf-tool v1.2 released.

* **2017-May-01**: mddf-tool v1.1.3.rc8 released as an executable jar for testing and evaluation.

* **2017-Apr-21**: mddf-tool v1.1.3.rc6 released as an executable jar for testing and evaluation.

* **2017-Apr-18**: mddf-tool v1.1.3.rc4 released as an executable jar for testing and evaluation.

* **2017-Mar-29**: mddf-tool released as an executable jar. Processing of either Avails or Manifest files is supported.

* **2017-Mar-02**: mddf-lib v1.1.2 released.

* **2017-Feb-13**: mddf-lib v1.1.1 released. This provides several fixes relating to the translation of Avails
from XLSX to XML. 

* **2017-Jan-30**: mddf-lib v1.1 released. 