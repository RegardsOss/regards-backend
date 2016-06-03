import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { getThemeStyles } from 'common/ThemeUtils';

class ProjectComponent extends React.Component {
  render(){
    const styles = getThemeStyles(this.props.theme, 'portalApp/project');
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
  project: React.PropTypes.object.isRequired
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(ProjectComponent);
