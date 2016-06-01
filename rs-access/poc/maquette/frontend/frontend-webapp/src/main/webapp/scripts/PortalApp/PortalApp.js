import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import CSSModules from 'react-css-modules';

import RegardInstance from './Projects/RegardInstance';
import RegardProjects from './Projects/RegardProjects';

import styles from 'PortalApp/base';


class PortalApp extends React.Component {
  render(){
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div styleName="main">
        <RegardInstance />
        Available projects on REGARDS instance :
        <RegardProjects />
      </div>
    )
  }
  }
}

module.exports = CSSModules(PortalApp, styles);
