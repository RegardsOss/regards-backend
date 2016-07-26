/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

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
        <Link to={"/user/" +this.props.project.name}>ihm user</Link>
        <Link to={"/admin/" +this.props.project.name}>ihm admin</Link>
      </li>
    )
  }
}

export default ProjectComponent
