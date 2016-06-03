import React from 'react';
import RegardsView from 'RegardsView';
import PluginView from 'common/pluginsManager/PluginView'

class PluginModule extends RegardsView {

  getDependencies(){
    const { project, plugin } = this.props;
    if (plugin && plugin.getDependencies){
      return plugin.getDependencies();
    } else {
      return null;
    }
  }

  renderView(){
    // this.props : parameters passed by react component
    // this.props.params : parameters passed by react router
    const { plugin } = this.props.params;
    return <PluginView name={plugin}/>
  }
}

PluginModule.propTypes = {
  params: React.PropTypes.object.isRequired
}

module.exports = PluginModule
