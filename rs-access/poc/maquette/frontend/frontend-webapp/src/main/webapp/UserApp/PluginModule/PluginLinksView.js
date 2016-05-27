import React from 'react';
import RegardsView from 'RegardsView';

import PluginLinkView from './PluginLinkView';

class PluginLinksView extends RegardsView {

  getDependencies(){
    return null;
  }

  renderView(){
    const { plugins, project } = this.props;
    return (
        <nav>
          {plugins.map( plugin => {
            if (plugin.name && plugin.plugin){
              return (
                <PluginLinkView key="{plugin.name}"
                  project={project}
                  plugin={plugin}/>
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
