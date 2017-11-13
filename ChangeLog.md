![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MDDF Change Log

---

## Versioning

The latest release of the core mddf-lib is v1.3. 

### mddf-lib Changes:

#### mddf-lib v1.3
* v1.3_rc4 has been released as v1.3

#### mddf-lib v1.3_rc4
* Bug Fix: Flag as an ERROR when Avails XLSX contains Avail in 3rd row.
* Bug Fix: Error handling when an Excel column is missing
* Bug Fix: BundledAssets in Collections were ignored when validating structure
* Bug Fix: Translation of Avails to XLSX dropped 'AvailMetadata:CaptionIncluded'
* BUG FIX: ReportingID was being dropped when converting Avails to XML
* BUG FIX: Avails XSLX compression (i.e. empty column hiding) now preserves all data formats
* Enhancement: Avails XLSX file may be re-formatted and cleaned up.

#### mddf-lib v1.3_rc3
* Bug Fix: Translation of Avails to XLSX dropped termNames 'SeasonWSP' and 'EpisodeWSP'
* Bug Fix: TPR terms were flagged as invalid in Avails XML v2.2 and Excel v1.7
* Bug Fix: Manifest validation did not check image resolutions were properly formatted.
* Bug fix: When using CLI, log file output did not specify name of file.
* Bug Fix: Manifest validation incorrectly required an Audiovisual/Presentation to have an ID
* Bug Fix: Manifest validation did not check value of Experience/App/Type
* Bug fix: Avails XLSX processing of episodic content used wrong form of Metadata
* Update: Avails format XLSX v1.6 has been DEPRECATED and is no longer supported.
* Update: support for Avails XLSX v1.7.2 added
* Update: support for Avails XML v2.2.2 added
* Update: support for Manifest v1.6.1 added
* Update: support for CM v2.6 added
* Update: Ratings DB updated to v2.3.1
* Other: non-compliance with Best Practice for ID syntax changed to a WARNING instead of ERROR
* Enhancement: Identified use of Excel formats other than .xlsx as a security risk.
* Enhancement: UI for Avails translation has improved layout and design.
* Enhancement: hide empty Excel columns in saved Avails spreadsheets
* Internal: increased flexibility of Structure Validation module
* Internal: refactoring of Avails Translator and TranslatorDialog to improve modularity.

#### mddf-lib v1.2
* No changes. Functionally equivalent to v1.1.3_rc8

#### mddf-lib v1.1.3_rc8
* Bug fix: When translating v1.6 XLSX Avails to v1.7 XLSX or v2.2 XML, StoreLanguage was not properly converted to AssetLanguage.
* Bug fix: When translating v1.6 XLSX Avails to v1.7 XLSX or v2.2 XML, HoldbackExclusionLanguage was not properly converted to AllowedLanguage.
* Enhancement: Inability to parse Avails in XLSX due to embedded objects or comments now results in log msgs clearly identifying problem.
* Enhancement: CLI support for translation of Avails

#### mddf-lib v1.1.3_rc6

* Bug fix: When translating XLSX Avails to XML any SharedEntitlements were dropped.
* Bug fix: When translating XLSX Avails to XML redundant ReleaseHistory elements were not removed
* Bug fix: When translating XML Avails to XLSX the spreadsheet for Movies was incorrectly identified as 'Movie'
* Bug fix: When translating XML Avails to XLSX the AvailID was always set to the TransactionID of the 1st Transaction.
* Modification: LicenseRightsDescription is now treated as freeform when validating Avails.

* Refactoring: Class AbstractValidator has been renamed CMValidator and is no longer abstract.
* Bug Fix: Excel-formatted TV Avails threw exception when processing ReleaseHistory for a Season.
* Bug Fix: CPE Profile IP-1 had incorrect identifier IP-01
* Bug Fix: Corrected bug introduced in v1.1.2 that prevented validation of Manifests based on v1.4 of the CMM schema.
* Bug Fix: MMC validation incorrectly determined number of Experiences in each ALIDExperienceMap
* Enhancement: processing of TV Avails specified in Excel v1.6 format now handles 'Exception(s)Flag' typo in the template
* Enhancement: support for MEC v2.5 added.

#### mddf-lib v1.1.2

* Bug Fix: Validation of controlled (i.e., enummerated) vocabulary did not correctly use appropriate schema version when checking.
* Bug Fix: Validation of language tags ignored case of multiple languages being specified.
* Bug Fix: Processing of TV Avails specified in Excel v1.6 format did not correctly identify version of template as v1.6
* Bug Fix: Validation of TV Avails specified in Excel v1.6 format did not check EpisodeAltID, SeasonAltID, and SeriesAltID


#### mddf-lib v1.1.1

* Bug Fix: When converting Avail from XLSX to XML multiple ReleaseHistory and Rating elements for a single Asset are now transfered even if defined on multiple rows.

#### mddf-lib v1.1

* Bug Fix: Avails Asset metadata usage was not properly validated.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly translate durations to xs:duration format.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle countryRegions.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle SeasonWSP and EpisodeWSP terms.
* Bug Fix: Conversion of Avails from XLSX to XML ignored 'TPR-' terms.
* Bug Fix: Conversion of Avails from XLSX to XML dropped last avail entry.
* Enhancement: Conversion of Avails from XML to XLSX is now supported. Supported conversions are:
  * from XML (v2.1 or v2.2) to XLSX v1.7
  * from XLSX (v1.6 or v1.7) to XML v2.2
  * from XLSX v1.6 to XLSX v1.7
* Update: Ratings are now validated using v2.3 of the Common Metadata Ratings (CMR) data-set.

