import React from 'react';
import RegardsView from 'RegardsView';
import PluginView from 'Common/PluginsManager/PluginView'

class PluginModule extends RegardsView {

  renderView(){
    const { plugin } = this.props.params;
    return <PluginView name={plugin}/>
  }
}

PluginModule.propTypes = {
  params: React.PropTypes.object.isRequired
}

module.exports = PluginModule
