import React from 'react';
import { IndexLink, Link } from 'react-router';

class PluginLinks extends React.Component {

  render(){
    const { plugins, project } = this.props;
    const style={"fontSize": "20px", margin: "0px 10px"};
    const activeStyle = { 'borderBottom':'2px solid Red' };
    return (
        <nav>
          {plugins.map( plugin => {
            if (plugin.name && plugin.plugin){
              return (
                <Link key="{plugin.name}"
                  to={project + "/plugins/" + plugin.name}
                  activeStyle={activeStyle}
                  style={style}
                  plugin={plugin.app}>
                  {plugin.name}
                </Link>
              )
            }
          })}
        </nav>
      )
  }
}

PluginLinks.contextTypes = {
  store: React.PropTypes.object
}

export default PluginLinks
