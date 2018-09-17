# Parameters configuration

## Description 
This configuration allows you to specify informations for a specific catalogue attribute.
For exemple if you have an attribute named MyAttribute in fragment named "Exemple" you can define this attribute as below :

 - Opensearch name of the parameter : publicName
 - Opensearch namespace of the parameter : exemple_ns
 - Enable generation of possible values : True
 - Limit number of possible values : 50
 - Full json path of the asscoiated REGARDS attribute : properties.Exemple.MyAttribute
 
In the above exemple the catalogue "MyAttribute" will be exposed in the opensearchDescriptor as "{exemple_ns:publicName}.
Also, into the descriptor will be provided for this attribute the first 50 values available.

## Usage

The namespace defined for the attribute is used to know how to search for attribute values in future search requests.
For exemple for **time extension** you have to define the two below attributes :

 - Opensearch name of the parameter : **start**
 - Opensearch namespace of the parameter : **time**
 - Enable generation of possible values : False
 - Limit number of possible values : -
 - Full json path of the asscoiated REGARDS attribute : properties.start_date
  
  
 - Opensearch name of the parameter : **end**
 - Opensearch namespace of the parameter : **time**
 - Enable generation of possible values : False
 - Limit number of possible values : -
 - Full json path of the asscoiated REGARDS attribute : properties.stop_date

## EXISTING EXTENSIONS ##
 - **Time** : To specify parameter configure them with "time" namespace.
 - **Geo** : To specify parameter configure them with "geo" namespace. By default geometry parameter is associated to properties.geometry of all entities. Nevertheless you can set the geometry parameter to use for this extension to an other attribute if you need to. 
 - **Regards** : To specify parameter configure them with "regards" namespace.
