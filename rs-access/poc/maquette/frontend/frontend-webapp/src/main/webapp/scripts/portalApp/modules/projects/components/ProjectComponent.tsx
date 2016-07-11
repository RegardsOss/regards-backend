import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

import { Project } from './ProjectTypes'

interface ProjectTypes {
  project: Project,
  styles: any
}

class ProjectComponent extends React.Component<ProjectTypes, any> {
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

export default ProjectComponent
