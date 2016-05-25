import React from 'react';
import PluginView from 'Common/PluginsManager/PluginView'

class PluginModule extends React.Component {

  constructor(){
    super();
  }

  render(){
    console.log("Rendering plugin",this.props.params.plugin);
    return <PluginView name={this.props.params.plugin}/>
  }
}

PluginModule.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

module.exports = PluginModule
