import React from 'react';

import Menu from './MenuContainer';
import Home from 'adminApp/modules/home/HomeModule'

class LayoutContainer extends React.Component {
  render(){
    const { project } = this.props;
    return (
      <div>
        <div className="navigation">
          <Menu project={project}/>
        </div>
        <div className="content">
          {this.props.content || <Home />}
        </div>
      </div>
    );
  }
}

export default LayoutContainer;
