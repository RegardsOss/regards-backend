/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

import { Project } from '../types/ProjectTypes'

export interface ProjectTypes {
  project: Project
}


/**
 * React component to display a project in the portal application
 *
 * @prop {Project} project Project to display
 */
export class ProjectComponent extends React.Component<ProjectTypes, any> {
  render(){
    return (
      <li>
        <p>{this.props.project.name}</p>
        <Link to={"/user/" +this.props.project.name}>
           <FormattedMessage id="project.user.access.link" />
        </Link>
        <Link to={"/admin/" +this.props.project.name}>
           <FormattedMessage id="project.admin.access.link" />
        </Link>
      </li>
    )
  }
}

export default ProjectComponent
