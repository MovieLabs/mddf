![screenshot1](../mddf-tools/docs/users/md/manifest/validator/v1.1/images/MLabs_header.jpg)
# MovieLabs Digital Distribution Framework—MDDF


## <a name="h_mddf-lib"> mddf-lib</a>
mddf-lib implements all core (i.e., *non-UI*) functionality that can be used to generate, validate, or transform MDDF files. Stable releases of the mddf-lib will have a two-part version or a three-part version ID. Interim development and evaluation (i.e., BETA) releases will include an "rcN" suffix. 

Examples:
* v1.3 or v2.0 identify stable releases
* v2.0.1.rc2 is a Beta release of v2.0.1
  
Starting with v1.5.1, releases of mddf-lib are available via the Maven Central Repository. 
Developers wishing to use mddf-lib in their own software therefore have two options: building from the source or adding a dependency to their pom.xml:

		<dependency>
			<groupId>com.movielabs</groupId>
			<artifactId>mddf-lib</artifactId>
			<version>${mddf.lib.version}</version>
		</dependency>

 