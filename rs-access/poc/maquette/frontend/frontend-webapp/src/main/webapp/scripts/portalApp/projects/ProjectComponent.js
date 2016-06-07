import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

class ProjectComponent extends React.Component {
  render(){
    const { styles } = this.props;
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
  project: React.PropTypes.object.isRequired,
  styles: React.PropTypes.object.isRequired
}

export default ProjectComponent;
