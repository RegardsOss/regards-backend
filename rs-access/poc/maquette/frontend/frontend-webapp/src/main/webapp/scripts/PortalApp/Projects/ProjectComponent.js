import React from 'react';
import CSSModules from 'react-css-modules';

import { Link } from 'react-router';

import styles from 'PortalApp/project';

class ProjectComponent extends React.Component {
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

ProjectComponent.propTypes = {
  project: React.PropTypes.object.isRequired
}

export default CSSModules(ProjectComponent, styles);
