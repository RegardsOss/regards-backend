# REGARDS Access frontend

This is the REGARDS Access frontend.

## Install

```
if npm -v < 3.10.7 {
    npm run preinstall
    npm install
} else {
    npm install
}
npm run test
```

## Build

```
npm run build:production
```

## Dependencies

- node
- babel etc..

## Architecture

- build/ : Webpack compilation directory
- node_modules/ : vendors
- src/
  - adminApp/ : Contains the admin web application
  - common/ : Contains the common modules.
    - access-rights/ :
    Contains the root AccessRightsComponent which allow to manage component access rights from REST dependencies.
    - plugins/ :
    Contains the PluginActions and PluginReducers which allow to load plugins if any.
    Contains the PluginComponent which allow to display a given loaded plugins.
    - store/ :
    Contains the Redux Store. Each module must register his reducers to the store.
- plugins/ : Contains all embeded plugins.
- portalApp/ : Contains the user web application
- userApp/ : Contains the user web applicaion
    - pluginModule/ :
    Contains the views to display plugins in the user app.
    - testModule/ :
    Contains a view example for access denied to REST resource.
    This view shall not be display.
- steelsheets/ : application styles (sass)


## Plugins


All plugins in plugins/ directory have to be React components.
See the maven project frontend-plugins to see plugin exemple HelloWorldPlugin
