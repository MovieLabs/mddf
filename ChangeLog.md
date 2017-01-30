![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MDDF Change Log

---

## Versioning

The latest STABLE release of the core mddf-lib is v1.1. The tool releases based on the latest version of the core software are: 

* Manifest Validator: v1.2.2
* Avails Validator: v1.1
 

### mddf-lib Changes for v1.1

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

### Avails Validator Changes

* Enhancement: Conversion of Avails from XML to XLSX is now supported.

### Manifest Validator Changes

Uses mddf-lib v1.1 but no changes to end functionality