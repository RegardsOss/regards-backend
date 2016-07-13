/** @module AdminProjectAdmins */
import * as React from 'react'
var icons = require('../../../../../stylesheets/foundation-icons/foundation-icons.scss')
import { map } from 'lodash'
// Styles
var classnames = require('classnames')

import { Project } from '../../projects/types/ProjectTypes'

interface ProjectAdminsProps {
  project:Project,
  styles: any,
  projectAdmins: Array<any>,
  onAddClick: (id: string) => void,
  onConfigureClick: (id: string) => void,
  onDeleteClick: (id: string) => void
}


/**
 * React component to display the list of administrators of a given project
 *
 * @prop {Project}     project          Project to display
 * @prop {Object}      styles           CSS styles
 * @prop {Array<User>} projectAdmins    List of administrators
 * @prop {Function}    onAddClick       Callback to add a new administrator
 * @prop {Function}    onConfigureClick Callback to configure an administrator
 * @prop {Function}    onDeleteClick    Callback to delete an administrator
 */
class ProjectAdminsComponent extends React.Component<ProjectAdminsProps, any> {
  render(){
    const { project, projectAdmins, styles, onAddClick, onConfigureClick,onDeleteClick } : any = this.props;
    if(project) {
      const className = classnames(styles['callout'], styles['custom-callout'])
      return (
        <div className={className}>
          Project Administrators
          <button title='Add new administrator' onClick={() => onAddClick(project.id)}>
            <i className={icons['fi-plus']} ></i>
          </button>
          <br />
          List of administrators for {project.name}:
          <ul>
            {map(projectAdmins.items, (projectAdmin: any, id: string) => (
              <li key={id}>
                {projectAdmin.name}
                <button title='Configure admin user' onClick={() => onConfigureClick(id)}>
                  <i className={icons['fi-wrench']}></i>
                </button>
                <button title='Delete admin user' onClick={() => onDeleteClick(id)}>
                  <i className={icons['fi-trash']}></i>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )
    } else {
      return null
    }
  }
}

export default ProjectAdminsComponent
