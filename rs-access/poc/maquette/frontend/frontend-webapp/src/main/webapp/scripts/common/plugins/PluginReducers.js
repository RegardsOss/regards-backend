import {
  REQUEST_PLUGINS,  RECEIVE_PLUGINS,
  FAILED_PLUGINS, PLUGIN_INITIALIZED } from './PluginsActions'

const plugins = (state = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action) => {
  switch(action.type){
    case REQUEST_PLUGINS:
      return Object.assign({}, state, {
        isFetching: true
      })
    case RECEIVE_PLUGINS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.plugins,
        lastUpdate: action.receivedAt
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

const pluginsReducers = {
  plugins
}

export default pluginsReducers
