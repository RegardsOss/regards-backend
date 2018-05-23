# How to use the AIPMisc Allocation Strategy plugin?

## Description 

This plugin reads information from the **miscInformation** bloc of the given **Submition Information Packages** (SIP) to define ditrectories where to store files.

## miscInformation format

```json
miscInformation : {
 storage: [
   "PluginId1":"/directory/storage/files/",
   "PluginId2":"/other/directory/storage/files/"
 ]
}
```

In the previous example, the **AIPMiscAllocationStrategyPlugin** will try to store each file from the Submition package into two data storage locations. The two data storage locations are :
 - **/directory/storage/files/** for the highest prioriterized **data storage configuration** of the storage plugin **PluginId1**.
 - **/other/directory/storage/files/** for the highest prioriterized **data storage configuration** of the storage plugin **PluginId2**.
 
Example with the **Local** data storage plugin (**Local** is the plugin identifier for the plugin to store into a local disk **LocalDataStorage**) :

```json
miscInformation : {
 storage: [
   "Local":"/directory/storage/files/",
 ]
}
```

## Full SIP example

```json
{
    "type": "FeatureCollection",
    "metadata": {
        "processing": "DefaultProcessingChain",
        "session": "AIPMiscAllocationStrategyPluginExample"
    },
    "features": [
        {
        	"type": "Feature",
			"ipType": "DATA",
			"id": "AIPMiscAllocationStrategyPluginExample",
			"geometry": null,
			"properties": {
				"contentInformations": [
					{
						"representationInformation": {
							"syntax": {
								"mimeType": "text/plain",
								"name": "TEXT"
							}
						},
						"dataObject": {
							"regardsDataType": "RAWDATA",
							"urls": [
								"file:/regards-input/file1.dat"
							],
							"algorithm": "MD5",
							"checksum": "779099efb274928ec6fade6c93c7877c"
						}
					}
				],
				"pdi": {
					"contextInformation": {
						"tags": [
							"DATASET_FOR_AIPMiscAllocationStrategyPluginExample"
						]
					},
					"referenceInformation": {},
					"provenanceInformation": {
						"history": []
					},
					"fixityInformation": {},
					"accessRightInformation": {}
				},
				"descriptiveInformation": {
					"creationdate": "2018-01-20T11:22:49Z",
					"label": "AIPMiscAllocationStrategyPluginExample Data 1",
				}
			},
		}
    ]
}
```


