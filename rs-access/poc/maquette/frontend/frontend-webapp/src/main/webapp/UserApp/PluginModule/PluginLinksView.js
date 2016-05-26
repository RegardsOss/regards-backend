import React from 'react';
import { IndexLink, Link } from 'react-router';
import RegardsView from 'RegardsView';

class PluginLinksView extends RegardsView {

  renderView(){
    const { plugins, project } = this.props;
    const style={"fontSize": "20px", margin: "0px 10px"};
    const activeStyle = { 'borderBottom':'2px solid Red' };
    return (
        <nav>
          {plugins.map( plugin => {
            if (plugin.name && plugin.plugin){
              return (
                <Link key="{plugin.name}"
                  to={"/user/" + project + "/plugins/" + plugin.name}
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

PluginLinksView.propTypes = {
  plugins: React.PropTypes.array.isRequired,
  project: React.PropTypes.string.isRequired
}

export default PluginLinksView
