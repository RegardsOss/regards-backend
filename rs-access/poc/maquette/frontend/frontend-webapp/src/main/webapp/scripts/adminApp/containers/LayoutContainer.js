import React from 'react';

import Menu from './MenuContainer';
import Home from 'adminApp/modules/home/HomeModule'

class LayoutContainer extends React.Component {
  render(){
    // Add location to menu props in order to update view if location
    // change. This enable the activeClassName to update in the react
    // router links.
    const { project, location } = this.props;
    return (
      <div>
        <div className="navigation">
          <Menu project={project} location={location}/>
        </div>
        <div className="content">
          {this.props.content || <Home />}
        </div>
      </div>
    );
  }
}

LayoutContainer.propsTypes = {
  location: React.PropTypes.object.isRequired,
  content: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired,
  instance: React.PropTypes.bool.isRequired,
}

export default LayoutContainer;
