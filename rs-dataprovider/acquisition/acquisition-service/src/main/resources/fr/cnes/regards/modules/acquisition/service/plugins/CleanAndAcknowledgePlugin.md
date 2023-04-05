# How to use the clean and acknowledge plugin ?

## Functionality Clean

> Removes all input files after scanning

Functionality disabled by default

## Functionality Create Acknowledge

> Create an ack file for all acquired files

Functionality disabled by default. This option creates a file with the exact same name plus the ack extension inside
an ack folder.
Example (with default config):

      for filename : toto.txt
      an ack file will be created : toto.txt.regards

These ack will be stored by default in the scanned folder.
That means that an ack folder will be created for each folder scanned. An option exists to store all ack file in a
single folder.
Example (with default config):

      for filepath scaned : /scanfolder/folder/toto.txt
      ackpath will be : /scanfolder/folder/ack_regards/toto.txt.regards

### Configuration of Create acknowledge functionality

* The folder name where ack will be stored is editable (default **ack_regards**)
* The ack file extension is editable (default **.regards**)
* Can create once ack folder at the scan root directory. If two scanned file have the same name, only one ack will be
  created. Example :

       for filepath scaned : /scanfolder/folder/toto.txt
       ackpath will be : /scanfolder/ack_regards/toto.txt.regards

* Before starting acquisition chain, a validation of the access right (access read and write) of all scan folders
  is done. By default, this validates only the first level of folder, and no sub-folder are checked (for performance
  reason). An option exists to force verificate recursively all folders (**don't use it for big directories**)


