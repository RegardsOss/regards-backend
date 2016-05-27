import React from 'react';
import { IndexLink, Link } from 'react-router';
import RegardsView from 'RegardsView';

class PluginLinkView extends RegardsView {

  getDependencies(){
    const { project, plugin } = this.props;
    if (plugin.getDependencies){
      return plugin.getDependencies();
    } else {
      return null;
    }
  }

  renderView(){
    const { project, plugin } = this.props;
    const style={"fontSize": "20px", margin: "0px 10px"};
    const activeStyle = { 'borderBottom':'2px solid Red' };
    return (
      <Link
        to={"/user/" + project + "/plugins/" + plugin.name}
        activeStyle={activeStyle}
        style={style}
        plugin={plugin.plugin}>
        {plugin.name}
      </Link>
      )
  }
}

PluginLinkView.propTypes = {
  plugin: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired
}

export default PluginLinkView
