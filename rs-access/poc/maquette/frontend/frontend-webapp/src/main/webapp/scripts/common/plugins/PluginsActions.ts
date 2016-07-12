import * as React from 'react'
import PluginType from './PluginTypes'
var { CALL_API } = require('redux-api-middleware')

const PLUGINS_API='http://localhost:8080/api/plugins'
export const REQUEST_PLUGINS = 'REQUEST_PLUGINS'
export const RECEIVE_PLUGINS = 'RECEIVE_PLUGINS'
export const FAILED_PLUGINS = 'FAILED_PLUGINS'

// Fetches plugins
export const fetchPlugins = () => ({
  [CALL_API]: {
    types: [
      REQUEST_PLUGINS,
      {
        type: RECEIVE_PLUGINS,
        meta: { receivedAt: Date.now() }
      },
      FAILED_PLUGINS
    ],
    endpoint: PLUGINS_API,
    method: 'GET'
  }
})

export const PLUGIN_INITIALIZED = 'PLUGIN_INITIALIZED'
export const pluginInitialized = (name:string, plugin:React.ComponentClass<any>) => ({
    type: PLUGIN_INITIALIZED,
    name: name,
    plugin: plugin,
    error: ''
})

// // TODO: Reactivate this?
// body.map( plugin => {
//   const paths = plugin.paths.map( path => {
//       return window.location.origin + "/plugins/" + path
//   })
//   scriptjs(paths, plugin.name)
// })
