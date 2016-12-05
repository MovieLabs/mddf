![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MovieLabs Digital Distribution Framework—MDDF

---

## Overview:

This repository contains Java software supporting the creation and usage of MDDF files including

* Avails
* Common Media Manifest (CMM)
* Media Manifest Core (MMC)
* Media Entertainment Core (MEC)

Information on the various MDDF standards is available at <http://www.movielabs.com/md/>

## Status and Relationship to Other MovieLabs Github Repositories:

The software in this repository is intended to replace that contained in the following MovieLabs repositories:

* availslib
* availstool
* cpe-preprocessing


## Software Organization & Roadmap:

There are two projects within this repository:

* __mddf-lib__: this implements all core (i.e., *non-UI*) functionality that can be used to generate, validate, or transform MDDF files.
* __mddf-tools__: implements standalone applications for validation.

The applications in mddf-tools are implemented on top of mddf-lib and any developers intending to develop their 
own mddf support applications are encouraged to do the same.

The mddf-tools software currently implements two applications:

   - **Avails Validator Tool**: may be used to
      - validate an Avails file specified as either XML or XLSX
      - translate an XLSX-formatted Avails to the equivalent XML
   
   - **Manifest Validator**: validates that a CMM, MMC, or MEC file conforms with:
     - the CMM schema
     - recommended 'Best Practices'
     - a specific *profile*
      
