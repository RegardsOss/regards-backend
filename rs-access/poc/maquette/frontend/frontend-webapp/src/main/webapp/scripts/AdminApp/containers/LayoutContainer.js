import React from 'react';

import Menu from './MenuContainer';
import HomeModule from 'AdminApp/modules/HomeModule/HomeModule'

class LayoutContainer extends React.Component {
  render(){
    return (
      <div>
        <div className="navigation">
          <Menu project={this.props.project}/>
        </div>
        <div className="content">
          {this.props.content || <HomeModule />}
        </div>
      </div>
    );
  }
}

LayoutContainer.contextTypes = {
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

export default LayoutContainer;
