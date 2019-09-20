# How to use the glob disk stream scanning plugin?

## Define directories to scan

This plugin only scans **local disk** directories using stream.
At least, one directory is required. You have to give the absolute path to the target directory.

For example,

```sh
/home/foo/bar
```

> During execution, all directories are scanned one after another and all files detected are returned excepted if a **last modification date** is passed to the scan method in which case only the most recent files are returned.

## Filter detected files with a glob pattern

The plugin may optionnally filter the detected file with a glob pattern.   
By default, the pattern `*` is used so all files without restriction are availables.   

> This filter is used in combination with last modification date.
