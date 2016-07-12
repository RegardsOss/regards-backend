import * as React from 'react'
import { connect } from 'react-redux'
import PluginType from './PluginTypes'
import { pluginInitialized } from './PluginsActions'

interface PluginProps {
  plugin: PluginType
}
/**
 Display the content of a plugin.
*/
class PluginComponent extends React.Component<PluginProps, any> {
  render(){
    const { plugin } = this.props
    if (plugin && plugin.plugin){
      return React.createElement(plugin.plugin,null)
    } else {
      return <div className="error"> Undefined plugin </div>
    }
  }
}

const mapStateToProps = (state) => ({})
const mapDispatchToProps = (dispatch) => ({
  pluginInitialized: (name, plugin) => dispatch(pluginInitialized(name, plugin))
})

export default connect()(PluginComponent)
