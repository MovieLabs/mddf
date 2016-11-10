Contents of this directory are used when packaging up the MDDF Validation tools 
as stand-alone (i.e., self-contained) Java applications. The Ant file build.xml
IN THIS DIRECTORY will manage the entire process:

1) The build (i.e., packaging) process must be performed on each type of
	platform (i.e., Mac OS-X, Linux, and Windows). Any package generated 
	will end up in the "${proj.home}/distro" directory.
	
2) The script will use ${proj.home}/packager as a TEMP directory used to stage
	the package contents. 

3) the build script will copy the contents of the ${proj.home}/dev/packaging/dist" 
	and ${proj.home}/dev/packaging/package directories to ${proj.home}/packager.
	
	======================================
To Prep:

	MAKE SURE that the version specified in the build.xml in THIS directory
	for BOTH ${app.version.cmm} and ${app.version.avail} is correct as 
	it will be used for file names.
	
	MAKE SURE correct version of mddf-lib is specified in the build.xml as this
	is used to determined which mddf-lib jar file is copied.
	
To Run:

   a) cd to this directory. Assuming the USB has name 'DATA01' then
       - on MacBook its /Volumes/DATA01/mddf/mddf-tools/dev/packaging
       - on Windows its E:\mddf\mddf-tools\dev\packaging
       - on Linux its /media/{user-name}/DATA01/mddf/mddf-tools/dev/packaging
       
   b) %> ant deploy-all
   
       The new distro will be found in the "${proj.home}/distro" directory.
       
       NOTE: when running on Mac OS-X, 
                1) the msg is "did not find a key matching 'developer id application"
                   can be ignored. The DMG file will still be created... it just
                   won't be acceptable to the Apple store.
                   
                2) from command line, run
                
                      chflags -R nouchg /PATH/TO/DIRECTORY/WITH/LOCKED/FILES/
                      
                   before running Ant if task 'deploy-init' fails.
       
   c) eject / unmount the USB 
   
   