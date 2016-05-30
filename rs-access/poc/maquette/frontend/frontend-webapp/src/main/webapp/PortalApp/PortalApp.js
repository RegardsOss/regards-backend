import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import RegardProjects from './Projects/RegardProjects';

import './stylesheets/base';

class PortalApp extends React.Component {
  render(){
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div>
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
        Available projects on REGARDS instance :
        <RegardProjects />
      </div>
    )
  }
  }
}

module.exports = PortalApp
