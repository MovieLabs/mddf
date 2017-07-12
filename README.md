![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MovieLabs Digital Distribution Framework—MDDF

1 [What's New](#h_News)

2 [Overview](#h_Overview)

3 [Status](#h_Status)

4 [Software Organization & Roadmap](#h_Roadmap)

5 [Executable Packages](#h_Versions)

6 [Past Releases](#h_History)

---
## <a name="h_News">What's New</a>

* **2017-May-09**: v1.2 is now available as either an executable jar or as native executables.

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

### Formats:

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
Stable releases of the mddf-lib will have a two-part version while evaluation (i.e., BETA) releases will have a three-part version ID. 
Interim development and alpha releases will include an "rcN" suffix. 

Executable jars are created for both stable and BETA releases and track the version number of the underlying mddf-lib.

Example: mddf-tool-v1.2.jar is based on, and compatible with, v1.2 of mddf-lib.

#### Native executables:
Native executables for the MDDF tools (i.e., Avails Validator and Manifest Validator) are only created using the stable mddf-lib 
releases. Begining with the release of v1.3 of mddf-lib, native executables are assigned versions that append a letter to the 
mddf-lib version.

Examples: 
* an Avails Validator with a version of v1.3.a is based on v1.3 of the mddf-lib.
* a Manifest Validator with a version of v1.3.2.c is based on v1.3.2 of the mddf-lib.

Prior to July 2017 and the release of v1.3 of the mddf-lib, the native executables were assigned versions a version ID
independant of the mddf-lib version. 

## <a name="h_History">Past Releases:</a>

* **2017-May-09**: mddf-tool v1.2 released.

* **2017-May-01**: mddf-tool v1.1.3.rc8 released as an executable jar for testing and evaluation.

* **2017-Apr-21**: mddf-tool v1.1.3.rc6 released as an executable jar for testing and evaluation.

* **2017-Apr-18**: mddf-tool v1.1.3.rc4 released as an executable jar for testing and evaluation.

* **2017-Mar-29**: mddf-tool released as an executable jar. Processing of either Avails or Manifest files is supported.

* **2017-Mar-02**: mddf-lib v1.1.2 released.

* **2017-Feb-13**: mddf-lib v1.1.1 released. This provides several fixes relating to the translation of Avails
from XLSX to XML. 

* **2017-Jan-30**: mddf-lib v1.1 released. 