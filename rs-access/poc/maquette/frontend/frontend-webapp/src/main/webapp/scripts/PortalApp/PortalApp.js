import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import RegardProjects from './Projects/RegardProjects';

import 'PortalApp/base';

class PortalApp extends React.Component {
  render(){
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className="portalApp">
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
        Available projects on REGARDS instance :
        <RegardProjects />
      </div>
    )
  }
  }
}

module.exports = PortalApp
