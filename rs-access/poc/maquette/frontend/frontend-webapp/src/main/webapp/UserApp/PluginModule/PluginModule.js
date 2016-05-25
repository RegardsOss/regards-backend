import React from 'react';
import RegardsModule from 'RegardsModule';
import PluginView from 'Common/PluginsManager/PluginView'

class PluginModule extends RegardsModule {

  constructor(){
    super();
  }

  render(){
    const { plugin } = this.props.params;
    console.log("Rendering plugin",plugin);
    return <PluginView name={plugin}/>
  }
}

PluginModule.propTypes = {
  params: React.PropTypes.object.isRequired
}

PluginModule.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

module.exports = PluginModule
