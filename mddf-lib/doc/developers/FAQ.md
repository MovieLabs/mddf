# mddf-lib Developer's FAQ

### 1. How do you add support for a new version of a mddf standard?

Step 1: add the resource files to the `com.movielabs.mddf.resources` package. At a minimum, this will include XSD files. Optionally, one or more JSON-formatted _vocab_ files and a _structure_ file may also be added.

Step 2: update the constants and enums in `com.movielabs.mddf.MddfContext`

Step 3:  `MddfContext` also needs to be updated to link the _primary_ mddf standards (i.e., Avails, Manifest, and MEC) to the correct version of the Common Metadata standard (see `getReferencedXsdVersions()`).

Step 4: if the new version is backwards compatible with a previous version's _vocab_ file, the linkage needs to specified in `XmiIngester.getVocabResource()`.

Step 5: if a _structure_ file is used during validation, the version to use will be specified in the Validator's `validateUsgage()` method.

Note that adding support for a new version of the Avails XLSX format requires a different set of steps (see below)

### 2. How do you add support for a new version of the Avails XLSX template?

Step 1: update the enum `AvailsSheet.Version`

Step 2: add any required code to `AvailsSheet.identifyVersion()`

Step 3: the code in `ValidationController.convertSpreadsheet()` determines which XML version is matched with an XLSX version when validating. This will also need to be updated.

Step 4: `XmlBuilder.makeXmlAsJDom()` must be updated to match the XLSX version to the correct
class of `AbstractRowHelper`. It may also be necessary to create a new subclass of `AbstractRowHelper`.


### 3. How do you add support for converting an Avails file to/from a new version of Avails?
If the user is to have the option of converting an Avails **to** the new version, support must also be added in the translator modules:

* `TranslatorDialog.addVersionSelectors()` to provide UI support
* `Translator.translateAvails()` to manage the conversion
* when adding a new XLSX version, resource file `Mappings.json` in package `com.movielabs.mddflib.avails.xlsx` specifies the rules for generating XLSX from XML.
* when adding a new XML version, conversion is handled by Java code in the 'Translator' class. 
Refer to `Translator.avails2_1_to_2_2()` for an example implementation.

### 4. How do you update the global ratings DB?

Add the XML file to the `com.movielabs.mddf.resources` package and then update `MddfContext.CUR_RATINGS_VER`