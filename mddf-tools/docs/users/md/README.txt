a/o Release 1.5.1, documentation has been re-organized. Prior to this, there were separate branches for the 
Avails and Manifest validators, each of which had version-specific sub-directories. The re-organized 
structure reverses this and has the version directory above the tool-specifc directory:

./tools:
    |
    +-userguide:
       |
       +-{Vn.n}:
           |
           +-index.html
           |
           +-UsersGuide.html
           |
           +-avails:
           |     |
           |     +-index.html
           |     |
           |     +-UsersGuide.html 
           |
           +-manifest:
                |
                +-index.html
                |
                +-UsersGuide.html 