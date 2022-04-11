# Parameters configuration

## Description

This configuration allows you to specify informations for a specific catalogue attribute. For example if you have an
attribute named MyAttribute in fragment named "Example" you can define this attribute as below :

- Opensearch name of the parameter : publicName
- Opensearch namespace of the parameter : example_ns
- Enable generation of possible values : True
- Limit number of possible values : 50
- Full json path of the associated REGARDS attribute : properties.Example.MyAttribute

In the above example the REGARDS attribute "MyAttribute" will be exposed in the opensearchDescriptor as "{example_ns:
publicName}. Also, the descriptor will provide the first 50 values available for this attribute.

## Usage

The namespace helps the client to identify the meaning of an attribute in the REGARDS catalog and allow criteria on it.  
Criteria and name of the attribute are explicited in the extension.  
For example for **time extension** you have to define the two below attributes :

- Opensearch name of the parameter : **start**
- Opensearch namespace of the parameter : **time**
- Enable generation of possible values : False
- Limit number of possible values : -
- Full json path of the associated REGARDS attribute : properties.start_date

- Opensearch name of the parameter : **end**
- Opensearch namespace of the parameter : **time**
- Enable generation of possible values : False
- Limit number of possible values : -
- Full json path of the associated REGARDS attribute : properties.stop_date

With these two attributes, the opensearchDescriptor.xml would contain in the search URL two parameters :  
`start={time:start}&end={time:end}`

Some example for **Earth Observation extension** :

- Opensearch name of the parameter : **instrument**
- Opensearch alias of the parameter : **Instrument**
- Opensearch namespace of the parameter : **eo**
- Enable generation of possible values : False
- Limit number of possible values : -
- Full json path of the associated REGARDS attribute : properties.InstrumentName

The opensearchDescriptor.xml would contain `Instrument={eo:instrument}`

## EXISTING EXTENSIONS ##

### Time extension ###

To declare an attribute under this extension, use parameter namespace "time".

Supported attributes are :
- start
- end
When this extension is active, these new criteria are supported:
- start
- end

### Geo extension ###

To declare an attribute under this extension, use parameter namespace "geo". 
By default geometry parameter is associated to properties.geometry of all entities.  
Nevertheless, you can set the geometry parameter to use for this extension to an other attribute if you need to.

With this extension actived, these new criteria are supported :
 - geometry
 - box
 - location
 - lon
 - lat
 - radius

### Earth Observation extension ###

To declare an attribute under this extension, use parameter namespace "eo".
Supported attributes are :
- productType
- doi
- platform
- platformSerialIdentifier
- instrument
- sensorType
- compositeType
- processingLevel
- orbitType
- spectralRange
- wavelength
- hasSecurityConstraints
- dissemination
- title
- parentIdentifier
- productionStatus
- acquisitionType
- orbitNumber
- swathIdentifier
- cloudCover
- snowCover
- lowestLocation
- highestLocation
- productVersion
- productQualityStatus
- productQualityDegradationTag
- processorName
- processingCenter
- creationDate
- modificationDate
- processingDate
- sensorMode
- archivingCenter
- processingMode
- availabilityTime
- acquisitionStation
- acquisitionSubType
- startTimeFromAscendingNode
- completionTimeFromAscendingNode
- illuminationAzimuthAngle
- illuminationZenithAngle
- illuminationElevationAngle
- polarisationMode
- polarizationChannels
- antennaLookDirection
- minimumIncidenceAngle
- maximumIncidenceAngle
- dopplerFrequency
- incidenceAngleVariation
- resolution

### Media extension ###

Add to the Opensearch results some links to access to product files (quicklook, thumbnail, rawdata...) 

### Regards extension ###

To declare an attribute under this extension, use parameter namespace "regards".  
It basically allows accessing REGARDS attributes and parsing basic criteria on these attributes.
By default, all REGARDS attributes are automatically added to this extension, if activated.
You can override the alias of an attribute if you want to by declaring an override.