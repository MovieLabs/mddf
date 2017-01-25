![screenshot1](mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MDDF Change Log

---

## Versioning

The latest STABLE release of the core mddf-lib is v1.0.4. The tool releases based on the latest version of the core software are: 

* Manifest Validator: v1.2.1
* Avails Validator: v1.0

The current DEVELOPMENT Release of the core mddf-lib is v1.1.

### mddf-lib Changes for v1.1

* Bug Fix: Avails Asset metadata usage was not properly validated.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly translate durations to xs:duration format.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle countryRegions.
* Bug Fix: Conversion of Avails from XLSX to XML did not correctly handle SeasonWSP and EpisodeWSP terms.
* Bug Fix: Conversion of Avails from XLSX to XML ignored 'TPR-' terms.
* Bug Fix: Conversion of Avails from XLSX to XML dropped last avail entry.
* Enhancement: Conversion of Avails from XML to XLSX is now supported.
* Update: Ratings are now validated using v2.3 of the Commom Metadata Ratings (CMR) data-set.

### Avails Validator Changes

* Enhancement: Conversion of Avails from XML to XLSX is now supported.

### Manifest Validator Changes

Uses mddf-lib v1.1 but no changes to end functionality