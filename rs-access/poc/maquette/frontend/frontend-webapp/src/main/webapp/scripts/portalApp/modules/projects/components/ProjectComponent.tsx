/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

import { Project } from '../types/ProjectTypes'

export interface ProjectTypes {
  project: Project,
  styles: any
}


/**
 * React component to display a project in the portal application
 *
 * @prop {Project} project Project to display
 * @prop {Object} styles CSS Styles
 */
export class ProjectComponent extends React.Component<ProjectTypes, any> {
  render(){
    // styles props is passed throught the react component creation
    const { styles } = this.props
    return (
      <li className={styles.link}>
        <p>{this.props.project.name}</p>
        <Link to={"/user/" +this.props.project.name} className={styles["project-link"]}>
          <FormattedMessage id="project.user.access.link" />
        </Link>
        <Link to={"/admin/" +this.props.project.name} className={styles["project-link"]}>
          <FormattedMessage id="project.admin.access.link" />
        </Link>
      </li>
    )
  }
}

export default ProjectComponent
