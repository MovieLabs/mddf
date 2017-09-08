# mddf-lib Developer's FAQ

### 1. How do you add support for a new version of a mddf standard?

Step 1: add the resource files to the `com.movielabs.mddf.resources` package. At a minimum, this will include XSD files. Optionally, one or more JSON-formatted _vocab_ files and a _structure_ file may also be added.

Step 2: update the constants and enums in `com.movielabs.mddf.MddfContext`

Step 3: if the new version is backwards compatible with a previous version's _vocab_ file, the linkage needs to specified in `XmiIngester.getVocabResource()`. `XmiIngester` also needs to be updated to link the _primary_ mddf standards (i.e., Avails, Manifest, and MEC) to the correct version of the Common Metadata standard (see `setManifestVersion()`, `setAvailVersion()`, etc).

Note that adding support for a new version of the Avails XLSX format requires a different set of steps (see below)

### 2. How do you add support for a new version of the Avails XLSX template?

Step1: update the enum `AvailsSheet.Version`

Step 2: add any required code to `AvailsSheet.identifyVersion()`

Step 3: the code in `ValidationController.convertSpreadsheet()` determines which XML version is matched with an XLSX version when validating. This will also need to be updated.

Step 4: `XmlBuilder.makeXmlAsJDom()` must be updated to match the XLSX version to the correct
class of `AbstractRowHelper`. It may also be necessary to create a new subclass of `AbstractRowHelper`.

Step 6: If the user is to have the option of converting an Avails **to** the new version, support must also be added in the translator modules:

* `TranslatorDialog.addVersionSelectors()` to provide UI support
* `Translator.translateAvails()` to perform the conversion

### 3. How do you update the global ratings DB?

Add the XML file to the `com.movielabs.mddf.resources` package and then update `MddfContext.CUR_RATINGS_VER`