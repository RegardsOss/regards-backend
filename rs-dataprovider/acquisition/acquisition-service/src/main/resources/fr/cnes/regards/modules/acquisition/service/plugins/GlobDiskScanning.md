# How to use the glob disk scanning plugin?

## Define directories to scan

This plugin only scans **local disk** directories.   
At least, one directory is required. You have to give the absolute path to the target directory.

For example,

```sh
/home/foo/bar
```

> During execution, all scan-directories are scanned one after another and all files detected are returned excepted
> if a **last modification date** is passed to the scan method in which case only the most recent files are returned.

> **Warning** : Sub-folders are not managed by this plugin, for example the file
> ```sh
>    /home/foo/bar/folder/file.txt
> ```
> will **not** be scanned.
> </br> To scan all file and folder recursively, use a DiskStreamScanning plugin like **RegexDiskStreamScanning**, or
> **GlobDiskStreamScanning**

## Filter detected files with a glob pattern

The plugin may optionally filter the detected file with a glob pattern.   
By default, the pattern `*` is used so all files without restriction are availables.

> This filter is used in combination with last modification date.
