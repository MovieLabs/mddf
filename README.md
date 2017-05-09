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

Information on the various MDDF standards is available at <http://www.movielabs.com/md/>

## <a name="h_Status">Status and Relationship to Other MovieLabs Github Repositories:</a>

The software in this repository is intended to replace that contained in the following MovieLabs repositories:

* availslib
* availstool
* cpe-preprocessing

## <a name="h_Roadmap">Software Organization & Roadmap:</a>

There are two projects within this repository:

* __mddf-lib__: this implements all core (i.e., *non-UI*) functionality that can be used to generate, validate, or transform MDDF files.
* __mddf-tools__: implements standalone applications for validation.

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

* as an executable Java jar which supports the processing of either Avails or Manifest files. Jar files may be downloaded from the ./binaries directory of this repository.

Note that the executable jar provides all of the capabilities available via the two native excutable packages. In addition, the jar provides a command
line interface (CLI) that may be used either from a terminal window or in conjunction with scheduled jobs (e.g., via crontab).

Refer to ChangeLog.md for a list of specific enhancements and bug fixes for any given release.

### Versioning:

The MDDF tools are assigned a three-part version number based on which version of the mddf-lib they are built on.

Example: an Avails Validator with a version of v1.3.2 is based on v1.2 of the mddf-lib.

Stable releases of the mddf-lib will have a two-part version while evaluation (i.e., beta) releases will have a three-part version ID. Native executables 
for the Avails Validator and Manifest Validator are only created using the stable mddf-lib releases.

## <a name="h_History">Past Releases:</a>

* **2017-May-01**: mddf-lib v1.1.3.rc8 released as an executable jar for testing and evaluation.

* **2017-Apr-21**: mddf-lib v1.1.3.rc6 released as an executable jar for testing and evaluation.

* **2017-Apr-18**: mddf-lib v1.1.3.rc4 released as an executable jar for testing and evaluation.

* **2017-Mar-29**: mddf-lib released as an executable jar. Processing of either Avails or Manifest files is supported.

* **2017-Mar-02**: mddf-lib v1.1.2 released.

* **2017-Feb-13**: mddf-lib v1.1.1 released. This provides several fixes relating to the translation of Avails
from XLSX to XML. 

* **2017-Jan-30**: mddf-lib v1.1 released. 