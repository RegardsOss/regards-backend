# JSON Generator documentation

## Getting started

To generate data, just create a JSON template as follow as a JSON file.


``` 
{
    "key1":"value1",
    "key2":"{{uuid()}}",
}
```

Each property referencing a function will be generated. All others will be kept as is!

A function must match the following pattern : `{{function(parameters)}}`

`parameters` represents a **comma separated list** of parameters. The list can be **empty**.

### Escape comma in parameter

You can use `single quote` to protect parameter if needeed : `function(param1, 'comma,separated,param')`.

### Reference property only

If a property is prefixed with a `hash tag (#)`, this property will be removed from generated random object.

For instance, template :

```json \
{
    "#id":"{{uuid()}}",
    "urn":"{{urn('TEST:%s',#id)}}",
}
```

... allows to generate object below removing `#id`.

```json \
{
    "urn":"..."
}
```

## Available functions

| Function                                                 | Generated values                            |  
|----------------------------------------------------------|---------------------------------------------|
| `boolean()`                                              | `true` or `false` |
| `double()`                                               | Double |
| `double(min,max)`                                        | Double between limits (<span style="color:#800080">Not implemented yet</span>) |
| `enum(x,y,z)`                                            | Random value from specified parameters |
| `float()`                                                | Float |
| `float(min,max)`                                         | Float between limits (<span style="color:#800080">Not implemented yet</span>) |
| `integer()`                                              | Integer |
| `integer(min,max)`                                       | Integer between limits |
| `long()`                                                 | Long |
| `long(min,max)`                                          | Long between limits (<span style="color:#800080">Not implemented yet</span>) |
| `now()`                                                  | Now with OffsetDateTime |
| `odt()`                                                  | OffsetDateTime |
| `odt(minInclusive,maxExclusive)`                         | OffsetDateTime between limits (min inclusive, max exclusive) |
| `string()`                                               | Alphanumeric string of 10 to 20 characters length |
| `string(maxLengthInclusive,maxLengthExclusive)`          | Alphanumeric string between length limits |
| `urn('format')`                                          | URN with random UUID applied formatted with String.format |
| `urn(format, refkey)`                                    | URN based on generated value at refkey JSON path formatted with String.format |
| `uuid()`                                                 | Random UUID |
| `seq()`                                                  | Sequence of Integer value from 0 |
| `seq(format)`                                            | Sequence of Integer value from 0 formatted with String.format |
