import * as React from 'react'
import { connect } from 'react-redux'
import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'
import PluginComponent from '../../../common/plugins/PluginComponent'


interface PluginProps {
  params: any,
  plugin: any,
  plugins: Array<any>
}

class PluginContainer extends AccessRightsComponent<PluginProps, any> {

  getDependencies(){
    const { plugin } = this.props
    if (plugin && plugin.getDependencies){
      return plugin.getDependencies()
    } else {
      return null
    }
  }

  render(){
    if (this.state.access === true){
      console.log("Rendering module")
      // this.props : parameters passed by react component
      // this.props.params : parameters passed by react router
      const { params, plugins } = this.props

      if (plugins){
        const plugin = plugins.find( plugin => {
          return plugin.name === params.plugin
        })

        // Get plugin from store
        return <PluginComponent plugin={plugin}/>
      }
    }
    return null
  }
}

const mapStateToProps = ( state:any ) => {
  return {
    plugins: state.plugins.items
  }
}

const pluginConnected = connect<{}, {}, PluginContainer>(mapStateToProps)(PluginContainer)
export default pluginConnected
module.exports = pluginConnected
