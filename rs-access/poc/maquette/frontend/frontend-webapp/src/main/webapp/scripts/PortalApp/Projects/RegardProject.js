import React from 'react';
import CSSModules from 'react-css-modules';

import Project from './RegardProject'
import { Link } from 'react-router';

import styles from 'PortalApp/project';

class RegardProject extends React.Component {
  render(){
    return (
      <li styleName="link">
        {this.props.project.label}&nbsp;-&nbsp;
        <Link to={"/user/" +this.props.project.label}>ihm user</Link>&nbsp;/&nbsp;
        <Link to={"/admin/" +this.props.project.label}>ihm admin</Link>
      </li>
    )
  }
}

RegardProject.propTypes = {
  project: React.PropTypes.object.isRequired
}

export default CSSModules(RegardProject, styles);
