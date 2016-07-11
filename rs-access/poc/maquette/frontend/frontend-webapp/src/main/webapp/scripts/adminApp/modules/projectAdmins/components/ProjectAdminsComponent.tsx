//import { React, PropTypes } from 'react'
//import * as React from 'react'
import * as React from 'react'
import '../../../../../stylesheets/foundation-icons/foundation-icons.scss'
import { map } from 'lodash'
// Styles
var classnames = require('classnames')


interface ProjectAdminsProps {
  project: any,
  styles: any,
  projectAdmins: Array<any>,
  onAddClick: (id: string) => void,
  onConfigureClick: (id: string) => void,
  onDeleteClick: (id: string) => void
}

class ProjectAdminsComponent extends React.Component<ProjectAdminsProps, any> {
  render(){
    const { project, projectAdmins, styles, onAddClick, onConfigureClick,onDeleteClick } : any = this.props;
    if(project) {
      const className = classnames(styles['callout'], styles['custom-callout'])
      return (
        <div className={className}>
          Project Administrators
          <button title='Add new administrator' onClick={() => onAddClick(project.id)}>
            <i className={'fi-plus'} ></i>
          </button>
          <br />
          List of administrators for {project.name}:
          <ul>
            {map(projectAdmins.items, (projectAdmin: any, id: string) => (
              <li key={id}>
                {projectAdmin.name}
                <button title='Configure admin user' onClick={() => onConfigureClick(id)}>
                  <i className={'fi-wrench'}></i>
                </button>
                <button title='Delete admin user' onClick={() => onDeleteClick(id)}>
                  <i className={'fi-trash'}></i>
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
