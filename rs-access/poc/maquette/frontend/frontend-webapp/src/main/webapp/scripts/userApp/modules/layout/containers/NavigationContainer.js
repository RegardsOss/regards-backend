import React from 'react';
import { connect } from 'react-redux';

import AccessRightsComponent from 'common/access-rights/AccessRightsComponent';
import LinkComponent from '../components/LinkComponent';

class NavigationContainer extends AccessRightsComponent {

  getDependencies(){
    return null;
  }

  render(){
    const { location, plugins, project } = this.props;
    if (this.state.access === true && plugins.items){
      return (
        <nav>
          <LinkComponent location={location} key="plop" to={"/user/"+project+"/test"}>Test de lien</LinkComponent>
          {plugins.items.map( plugin => {
            if (plugin && plugin.plugin){
              return (
                <LinkComponent
                  location={location}
                  key={plugin.name}
                  to={"/user/" + project + "/plugins/" + plugin.name}>
                  {plugin.name}
                </LinkComponent>
              )
            }
          })}
        </nav>
      )
    }
    return null;
  }
}

NavigationContainer.propTypes = {
  project: React.PropTypes.string.isRequired,
  location: React.PropTypes.object.isRequired,
}

// Add projects from store to the container props
const mapStateToProps = (state) => {
  return {
    plugins: state.plugins
  }
}
module.exports = connect(mapStateToProps)(NavigationContainer);
