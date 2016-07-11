import * as React from 'react'

export type PluginType = {
  name: string,
  plugin: React.ComponentClass<any>,
  paths: Array<string>
}

export type PluginsStore = {
  isFetching:boolean,
  items: Array<PluginType>,
  lastUpdate: string
}

export default PluginType
