import React from 'react'
import { connect } from 'react-redux'

/**
 Display the content of a plugin.
*/
class PluginComponent extends React.Component {
  render(){
    const { plugin } = this.props
    if (plugin && plugin.plugin){
      return React.createElement(plugin.plugin,null)
    } else {
      return <div className="error"> Undefined plugin {plugin.name} </div>
    }
  }
}

export default connect()(PluginComponent)
