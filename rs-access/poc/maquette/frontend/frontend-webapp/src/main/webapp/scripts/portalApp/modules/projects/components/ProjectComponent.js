import React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

class ProjectComponent extends React.Component {
  render(){
    // styles props is passed throught the react component creation
    const { styles } = this.props
    return (
      <li className={styles.link}>
        <p>{this.props.project.name}</p>
        <Link to={"/user/" +this.props.project.name} className={styles["project-link"]}>ihm user</Link>
        <Link to={"/admin/" +this.props.project.name} className={styles["project-link"]}>ihm admin</Link>
      </li>
    )
  }
}

ProjectComponent.propTypes = {
  project: React.PropTypes.object.isRequired,
  styles: React.PropTypes.object.isRequired
}

export default ProjectComponent
