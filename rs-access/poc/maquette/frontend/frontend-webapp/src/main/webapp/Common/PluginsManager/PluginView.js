import React from 'react';

/**
 Display the content of a plugin.
*/
class PluginView extends React.Component {
  render(){
    const plugins = this.context.store.getState().plugins;
    const pluginToDisplay = this.props.name;
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

PluginView.contextTypes = {
  store: React.PropTypes.object
}

export default PluginView
