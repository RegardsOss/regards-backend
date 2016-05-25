import React from 'react';

/**
 Display the content of a plugin.
*/
class PluginApp extends React.Component {
  render(){
    const plugins = this.context.store.getState().plugins;
    const pluginToDisplay = this.props.params.name;
    const plugin = this.context.store.getState().plugins.find((curent) => {
      if (pluginToDisplay === curent.name){
        return curent;
      }
    });

    if (plugin && plugin.plugin){
      return React.createElement(plugin.plugin,null);
    } else {
      return <div className="error"> Undefined plugin {pluginToDisplay} </div>;
    }
  }
}

PluginApp.contextTypes = {
  store: React.PropTypes.object
}

export default PluginApp
