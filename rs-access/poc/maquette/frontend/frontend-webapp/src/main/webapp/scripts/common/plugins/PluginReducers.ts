import {
  REQUEST_PLUGINS,  RECEIVE_PLUGINS,
  FAILED_PLUGINS, PLUGIN_INITIALIZED } from './PluginsActions'
import { PluginsStore } from './PluginTypes'
import scriptjs from 'scriptjs'

export default (state:PluginsStore = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action: any) => {
  switch(action.type){
    case REQUEST_PLUGINS:
      return Object.assign({}, state, {
        isFetching: true
      })
    case RECEIVE_PLUGINS:
      // TODO: Find somewhere else to handle this
      action.payload.map( plugin => {
        const paths = plugin.paths.map(path => window.location.origin + "/plugins/" + path)
        scriptjs(paths, plugin.name)
      })
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload,
        lastUpdate: action.meta.receivedAt
      })
    case FAILED_PLUGINS:
      return Object.assign({}, state, {
        isFetching: false
      })
    case PLUGIN_INITIALIZED:
      let result = Object.assign({}, state)
      result.items = result.items.map( plugin => {
        return Object.assign({}, plugin, {
          plugin: action.plugin
        })
      })
      return result
    default:
      return state
  }
}
