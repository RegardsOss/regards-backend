import * as React from 'react'
import { connect } from 'react-redux'
import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'
import PluginComponent from '../../../common/plugins/PluginComponent'

import { PluginType } from '../../../common/plugins/PluginTypes'


interface PluginProps {
  params: any,
  plugin: PluginType,
  plugins: Array<any>
}

class PluginContainer extends React.Component<PluginProps, any> {

  render(){
    // this.props : parameters passed by react component
    // this.props.params : parameters passed by react router
    const { params, plugins } = this.props

    if (plugins){
      const plugin = plugins.find( plugin => {
      return plugin.name === params.plugin
      })
      // Get plugin from store
      return <PluginComponent plugin={plugin}/>
    } else {
      return null
    }
  }
}

const mapStateToProps = ( state:any ) => {
  return {
    plugins: state.common.plugins.items
  }
}

const pluginConnected = connect<{}, {}, PluginContainer>(mapStateToProps)(PluginContainer)
export default pluginConnected
