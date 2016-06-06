import React from 'react';
import { connect } from 'react-redux';

import AccessRightsComponent from 'common/modulesManager/AccessRightsComponent';
import PluginLinkView from './PluginLinkView';

class PluginLinksView extends AccessRightsComponent {

  getDependencies(){
    return null;
  }

  renderView(){
    const { plugins, project } = this.props;
    if (plugins.items){
      return (
        <nav>
          {plugins.items.map( plugin => {
            if (plugin && plugin.plugin){
              return (
                <PluginLinkView key={plugin.name}
                  project={project}
                  plugin={plugin}/>
              )
            }
          })}
        </nav>
      )
    } else {
      return null;
    }
  }
}

PluginLinksView.propTypes = {
  project: React.PropTypes.string.isRequired
}

const mapStateToProps = (state) => {
  return {
    plugins: state.plugins
  }
}
module.exports = connect(mapStateToProps)(PluginLinksView);
