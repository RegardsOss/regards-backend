import React from 'react';
import CSSModules from 'react-css-modules';

import { Link } from 'react-router';

import styles from 'PortalApp/project';

class ProjectComponent extends React.Component {
  render(){
    return (
      <li styleName="link">
        {this.props.project.name}&nbsp;-&nbsp;
        <Link to={"/user/" +this.props.project.name}>ihm user</Link>&nbsp;/&nbsp;
        <Link to={"/admin/" +this.props.project.name}>ihm admin</Link>
      </li>
    )
  }
}

ProjectComponent.propTypes = {
  project: React.PropTypes.object.isRequired
}

export default CSSModules(ProjectComponent, styles);
