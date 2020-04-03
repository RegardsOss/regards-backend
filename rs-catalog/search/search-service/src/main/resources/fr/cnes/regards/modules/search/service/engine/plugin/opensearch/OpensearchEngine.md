# Opensearch engine

## Description

The opensearch engine protocol allows to :
 - Retrieve opensearch descriptor in xml format
 - Run searches based on descriptor and opensearch/opensearch parameters standard.

 **Official specifications :**
 - [Opensearch standard](http://www.opensearch.org/Specifications/OpenSearch/1.1)
 - [Opensearch  parameters standard](http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2)
 
 **Opensearch extensions enabled :**
 - [Time extension](http://www.opensearch.org/Specifications/OpenSearch/Extensions/Time/1.0/Draft_1)
 - [Geo extension](http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_1)
 
## Usage

 Parameters to configure :
 - **Title** : Title write in opensearch researches metadatas
 - **Description** : Description write in opensearch researches metadatas
 - **Contact** : Email write in opensearch researches metadatas
 - **Tags** [optional] : Tags added in opensearch descriptor.
 - **Short name** : Engine short name write in opensearch descriptor
 - **Long name** : Engine Long name write in opensearch descriptor
 - **Image** [Optional] : Url to accessible image to add in the descriptor
 - **Attribution** : Attribution to wirte in descritor
 - **Path to last update date attribute** : Path like 'properties.last_update' to the catalog attribute used to write last update date of entities in search responses.
 - **Time&geo extension activation** : [True|false] 
 - **Media extension activation** : [True|False] add media links to entity like thumbnail, rawdatas, documents, ...
 - **Regards extension activation** : Add all project catalogue attributes to opensearch descriptors in "regards" namespace.
 - **Parameters configuration** : Allow you to specify each regards catalog atribute to match a namespace and a name for a specific extension.