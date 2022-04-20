![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MDDF Change Log

---

## Versioning

### mddf-lib 2.0.3.1
* SECURITY: Updated dependencies to remove flagged vulnerabilities in several plugins.

### mddf-lib 2.0.3

* Enhancement: Upgraded to support
  - Manifest v1.11
  - Media Ent. Core v2.10
  - Asset Order and Deliver v1.2
  - Common Metadata v2.10
  - Avails v2.6
* Warning: Does not support (but does flag)
  - SequenceParsing-type
  - Automated Review of Multiple TargetAudience-types in multiple LocalizedInfo types.

### mddf-lib 2.0.2

* Enhancement: updated Ratings DB to v2.4.8

### mddf-lib 2.0.1

* SECURITY: Removed vulnerabilities associated with use of Apache libraries LOG4J.

### mddf-lib 2.0

* SECURITY: Removed vulnerabilities associated with use of Apache libraries LOG4J and XMLBEANS.
* Enhancement: updated Ratings DB to v2.4.7

### mddf-lib 1.9.01

* Bug Fix: Avails 2.5.2 schema was missing

### mddf-lib 1.9

* Enhancement: Ratings DB updated. Now using v2.4.3
* Enhancement: Added support for processing of Asset Order and Delivery (AOD) files
* Enhancement: Added support for processing of Order Status files
* Enhancement: Upgraded to support
  - Manifest v1.10
  - Media Ent. Core v2.9
  - Asset Order and Delivery V1.1
  - Common Metadata v2.9

### mddf-lib 1.8.1

* Enhancement:  Ratings DB updated. Now using v2.4.2
* Bug Fix: Avails VolumeMetadata/Status was not being checked

### mddf-lib 1.8.0.2

* Bug Fix: runing in non-interactive mode resulted in a NullPointerException in the logger.

### mddf-lib 1.8.0.1

* Bug Fix: Ratings DB updated to fix to broken links in newly added Rating Systems.

### mddf-lib 1.8

* Enhancement: added support for Manifest v1.9, Avails v2.5/v1.9, MDMEC v2.8, and CM v2.8
* Enhancement: added support for validating Offer Status files.
* Enhancement: updated Ratings DB to v2.4
* User Interface: Avails VersionChooser dialog now displays file name
* Bug Fix: Ingest of XLSX Avails incorrectly identified AvailType when WorkType=Collection
* Bug Fix: Ingest of XLSX Avails failed due of typo in code identifying file type
* Bug Fix: Setting LOG-LEVEL to 'Debug' threw NullPtrException during initialization [NOTE: bug introduced in v1.7]
* Bug Fix: Ingest of Avails in XLSX dropped any term added from Avail v1.7.3 on (e.g. 'Download, Bonus) [NOTE: bug introduced in v1.6]

### mddf-lib 1.7 

* Enhancement: structure check may specify a XPath constraint to be applied to a supporting MEC file.
      Change made in support of BUG FIX re use of MEC files w/in a Manifest.
* Enhancement: absence of TitleSort or Summary190 in MEC v2.6 or later results in NOTICE re backward compatibility
* UI: Edit button removed from Validator tool bar.
* Bug Fix: fix incorrect validation of ExternalManifestID uniqueness and xrefs 
* Bug Fix: Avails XLSX ingest was being aborted by sheets not in conformance with template (e.g., data in extraneous columns)
* Bug Fix: MMC structure validation was incomplete when MEC file used to provide metadata.
* Bug Fix: Correct use of Audio/SubType not verified when present in a MEC file.
* Internal: use of static methods and fields in Validator classes reduced to allow simultaneous instantiation
       of multiple Validators. Change made in support of BUG FIX re use of MEC files w/in a Manifest.
* Internal: change to LogMgmt API allows simultaneous instantiation of multiple Validators targeting different files
* Internal: changes in MecValidator and ManifestValidator method access to allow sub-classing by cloud-specific extensions

### mddf-lib v1.6 

* Enhancement: Memory usage when ingesting large XLSX-formated Avails has been significantly reduced.
* Enhancement: Identification of ArtReference by means other that ImageID generates Best Practice notice
* User Interface: Manifest Profiles are no longer enterable via UI. Feature removed due to schema enhancements,
* Bug Fix: MMC detection of extra 'root' Experiences.
* Bug Fix: Eliminated memory leak when clearing log.
* Internal: CPE Validation now uses JSON-based structure checks
* Internal: re-organization of unit test classes
* Internal: `AvailsWrkBook.convertSpreadsheet()` has been deprecated in favor of using the `StreamingXmlBuilder`

### mddf-lib v1.5.3

#### rc1
* Bug Fix: MEC validation was incomplete due to multiple issue
   * terminology checks for DigitalAssets was not invoked
   * structural checks used incorrect namespace URI in XPaths
* Bug Fix: CoreMetadata included in an Avails did not get validated
* Enhancement: Improved UI for handling update notifications
* Internal: validation of DigitalAssets handled by CMValidator instead of ManifestValidator

### mddf-lib v1.5.2

#### rc2
* Bug Fix: translation of Avails Volume properties dropped several fields

#### rc1
* Bug Fix: allow use of LicenseFee to meet TVOD-based LicenseType requirement
* Bug Fix: xfer of SeriesContentID from XLSX to XML was missing
* Enhancement: added full support for ingest of Avails v1.8 Volume data
* Enhancement: allow use of "Custom:"  rating system.
* Enhancement: full support of CM 2.7.1 usage
* Enhancement: explicit check of namespace declarations in all XML headers 
* Enhancement: support for Avails v 2.4 added
* Enhancement: added support for translating Avails v2.4 to/from v1.8 
* Other: added ability to cancel XLSX-related processing via the VersionChooserDialog

### mddf-lib v1.5.1.

#### rc6

* Bug Fix: Validation of sequences and indices now allows non-zero values on case-by-case basis
* Bug Fix: correct relative positioning of top-level UI frames
* Enhancement: Allow on-demand checking for updates to software
* Enhancement: Added support for Avails v1.8

#### rc5

* Change: removed 'main' methods from AvailTool and ManifestTool; Use of ToolLauncher is now required
* Enhancement: When translating an Avail to XML, insert a comment providing audit trail
* Enhancement: Prompt user for Avails version when processing XLSX file
* Enhancement: Automatic checking for updates to software
* Internal: Added JUnit testing of Avails format conversion
* Internal: Reorganization of properties into generic and tool-specific sets

#### rc4

* Bug Fix: WARNINGS due to structure checks were invalidating file
* Bug Fix: Translation of Avails to XML dropped any AltID source that used an EIDR
* Bug Fix: Translation of Avails to XML dropped AvailMetadata/EpisodeTitleID

#### rc3

* Enhancement: CM v2.7.1 and Manifest v 1.8.1 supported in DRAFT format
* Bug Fix: Launching of internal XML Editor required lower case file-type suffix
* Internal: re-implemented MMC Validator to focus on JSON structures

#### rc2

* Bug Fix: added missing valiadtion checks for all DigitalAsset types
* KNOWN BUG: consistency of Encoding/ChannelMapping is not checked when the number
             of Audio channels is 1.

#### rc1
* Bug Fix: structure check of RefALID threw XML parse exception
* Internal: Expanded scope of JUnit testing of structure validator

### mddf-lib v1.5

* Enhancement: support for Manifest v 1.8 added
* Enhancement: support for CM v2.7 and MDMEC 2.7 added
* Enhancement: currency codes checked for conformance with ISO-4217
* Bug Fix: When mapping Avails to/from Excel, the GroupingEntity/Type should be 'channel', not 'SVOD channel'
* Bug Fix: cross-references from TextGroup/TextObjectID to TextObject were not being checked
* Bug Fix: cross-references from ExperienceChild to ExternalManifest were not being checked
* Bug Fix: Avails Transaction with TVOD-based LicenseType and no pricing Term was flagged as ERROR (now a WARNING)
* Bug Fix: All violations of structure constraints were flagged as ERROR regardless of actual severity level.
* Bug Fix: when indexing is optional, index usage must be consistent for all child elements
* Bug Fix: Verify exactly 1 instance of TrackMetadata with TrackSelectionNumber='0' per Presentation"
* Bug Fix: Eliminate race-condition in initialization of code validating ID types.
* Bug Fix: non-MDDF files (e.g. jpg) were included in list of supporting files to validate
* Internal: refactoring of code validating cross-referencing
* Internal: migrated mddf-lib testing to use JUnit5

#### mddf-lib v1.5_rc1
* Enhancement: Log file can now be exported as XML
* Bug Fix: All Avail conversions to an XLSX format failed due to improper identification of target format

### mddf-lib v1.4
* Enhancement: support for Avails v1.7.3 
* Enhancement: support for translating to/from Avails v2.3 added 
* Enhancement: any information removed when translating between versions gets logged (severity level = 'Notice')
* Enhancement: allow, in addition to Yes/No, either Y/N, T/F, or True/False as boolean values 

#### _v1.4.rc3_
* Bug Fix: v1.7.3 XLSX ingest dropped SeriesID, SeasonID and non-EIDR EditID and TitleID
* Bug Fix: Avails validation did not allow TPR prefix for LicenseFee and Category terms
 
#### *v1.4.rc2*
* Bug Fix: XLSX ingest of local file required WRITE access to close resource handle.
* Bug Fix: handling XLSX cell with invalid boolean value did not report error correctly
* Bug Fix: XLSX Avails without AssetLanguage threw Null Pointer Exception
* BUG FIX: re XLSX Avails v1.7.3 mapping of EIDR vs non-EIDR EditID and TitleID

#### v1.4.rc1_
* Bug Fix: Conversion of XLSX Avails to XML dropped FormatProfile attributes
* Bug Fix: Validation of controlled vocabulary did not check attributes (e.g., Avails termNames)
* Bug Fix: Processing of XLSX Avails dropped Start or End values that included time values.
* Bug Fix: Processing of XLSX Avails for Movies dropped CompanyDisplayCredit.
* Bug Fix: Conversion to XLSX Avails from either XML or another version of XLSX incorrectly processed ReportingID
* Bug Fix: Manifest validation incorrectly flagged AppGroup and Interactive  elements as unreferenced.

### mddf-lib v1.3.2
* Enhancement: POM file for Maven builds added.
* Enhancement: Added APIs to validation functions to support cloud-based services.
* Enhancement: Additions to logging utilities to support cloud-based services.
* Bug Fix: Conversion of XLSX Avails to XML incorrectly rounded Transaction start and end dates.
* Internal: refactoring of utilities used to convert dates and times to/from XSD syntax

### mddf-lib v1.3.1
* Enhancement: support for Avails v2.3 added
* Enhancement: support for Manifest v1.7 added
* Enhancement: support for MDMEC v2.6 added
* Deprecation: support for translation to Avails v2.2.2 has been removed
* Internal: added functions to support advanced XSD usage

### mddf-lib v1.3
* v1.3_rc4 has been released as v1.3

#### *v1.3_rc4*
* Enhancement: Avails XLSX file may be re-formatted and cleaned up.
* Bug Fix: Flag as an ERROR when Avails XLSX contains Avail in 3rd row.
* Bug Fix: Error handling when an Excel column is missing
* Bug Fix: BundledAssets in Collections were ignored when validating structure
* Bug Fix: Translation of Avails to XLSX dropped 'AvailMetadata:CaptionIncluded'
* BUG FIX: ReportingID was being dropped when converting Avails to XML
* BUG FIX: Avails XSLX compression (i.e. empty column hiding) now preserves all data formats

#### *v1.3_rc3*
* Enhancement: Identified use of Excel formats other than .xlsx as a security risk.
* Enhancement: UI for Avails translation has improved layout and design.
* Enhancement: hide empty Excel columns in saved Avails spreadsheets
* Enhancement: support for Avails XLSX v1.7.2 added
* Enhancement: support for Avails XML v2.2.2 added
* Enhancement: support for Manifest v1.6.1 added
* Enhancement: support for CM v2.6 added
* Enhancement: Ratings DB updated to v2.3.1
* Bug Fix: Translation of Avails to XLSX dropped termNames 'SeasonWSP' and 'EpisodeWSP'
* Bug Fix: TPR terms were flagged as invalid in Avails XML v2.2 and Excel v1.7
* Bug Fix: Manifest validation did not check image resolutions were properly formatted.
* Bug fix: When using CLI, log file output did not specify name of file.
* Bug Fix: Manifest validation incorrectly required an Audiovisual/Presentation to have an ID
* Bug Fix: Manifest validation did not check value of Experience/App/Type
* Bug fix: Avails XLSX processing of episodic content used wrong form of Metadata
* Deprecation: Avails format XLSX v1.6 has been DEPRECATED and is no longer supported.
* Other: non-compliance with Best Practice for ID syntax changed to a WARNING instead of ERROR
* Internal: increased flexibility of Structure Validation module
* Internal: refactoring of Avails Translator and TranslatorDialog to improve modularity.

### mddf-lib v1.2
* No changes. Functionally equivalent to v1.1.3_rc8

#### *v1.1.3_rc8*
* Enhancement: Inability to parse Avails in XLSX due to embedded objects or comments now results in log msgs clearly identifying problem.
* Enhancement: CLI support for translation of Avails
* Bug fix: When translating v1.6 XLSX Avails to v1.7 XLSX or v2.2 XML, StoreLanguage was not properly converted to AssetLanguage.
* Bug fix: When translating v1.6 XLSX Avails to v1.7 XLSX or v2.2 XML, HoldbackExclusionLanguage was not properly converted to AllowedLanguage.

#### *v1.1.3_rc6*

* Enhancement: processing of TV Avails specified in Excel v1.6 format now handles 'Exception(s)Flag' typo in the template
* Enhancement: support for MEC v2.5 added.
* Bug fix: When translating XLSX Avails to XML any SharedEntitlements were dropped.
* Bug fix: When translating XLSX Avails to XML redundant ReleaseHistory elements were not removed
* Bug fix: When translating XML Avails to XLSX the spreadsheet for Movies was incorrectly identified as 'Movie'
* Bug fix: When translating XML Avails to XLSX the AvailID was always set to the TransactionID of the 1st Transaction.
* Bug Fix: Excel-formatted TV Avails threw exception when processing ReleaseHistory for a Season.
* Bug Fix: CPE Profile IP-1 had incorrect identifier IP-01
* Bug Fix: Corrected bug introduced in v1.1.2 that prevented validation of Manifests based on v1.4 of the CMM schema.
* Bug Fix: MMC validation incorrectly determined number of Experiences in each ALIDExperienceMap
* Internal: LicenseRightsDescription is now treated as freeform when validating Avails.
* Internal: Class AbstractValidator has been renamed CMValidator and is no longer abstract.

### mddf-lib v1.1.2
* Bug Fix: Validation of controlled (i.e., enummerated) vocabulary did not correctly use appropriate schema version when checking.
* Bug Fix: Validation of language tags ignored case of multiple languages being specified.
* Bug Fix: Processing of TV Avails specified in Excel v1.6 format did not correctly identify version of template as v1.6
* Bug Fix: Validation of TV Avails specified in Excel v1.6 format did not check EpisodeAltID, SeasonAltID, and SeriesAltID

### mddf-lib v1.1.1

* Bug Fix: When converting Avail from XLSX to XML multiple ReleaseHistory and Rating elements for a single Asset are now transfered even if defined on multiple rows.

### mddf-lib v1.1
* Enhancement: Conversion of Avails from XML to XLSX is now supported. Supported conversions are:
  * from XML (v2.1 or v2.2) to XLSX v1.7
  * from XLSX (v1.6 or v1.7) to XML v2.2
  * from XLSX v1.6 to XLSX v1.7
* Bug Fix: Avails Asset metadata usage was not properly validated.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly translate durations to xs:duration format.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle countryRegions.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle SeasonWSP and EpisodeWSP terms.
* Bug Fix: Conversion of Avails from XLSX to XML ignored 'TPR-' terms.
* Bug Fix: Conversion of Avails from XLSX to XML dropped last avail entry.
* Update: Ratings are now validated using v2.3 of the Common Metadata Ratings (CMR) data-set.

