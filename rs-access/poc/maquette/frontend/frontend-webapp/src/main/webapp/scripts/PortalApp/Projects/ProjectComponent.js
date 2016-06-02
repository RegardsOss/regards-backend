import React from 'react';
import { Link } from 'react-router';

import { getThemeStyles } from 'Common/ThemeUtils';

class ProjectComponent extends React.Component {
  render(){
    const styles = getThemeStyles(this.props.theme, 'PortalApp/project');
    return (
      <li className={styles.link}>
        {this.props.project.name}&nbsp;-&nbsp;
        <Link to={"/user/" +this.props.project.name}>ihm user</Link>&nbsp;/&nbsp;
        <Link to={"/admin/" +this.props.project.name}>ihm admin</Link>
      </li>
    )
  }
}

ProjectComponent.propTypes = {
  theme: React.PropTypes.string.isRequired,
  project: React.PropTypes.object.isRequired
}

export default ProjectComponent;
