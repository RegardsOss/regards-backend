//import { React, PropTypes } from 'react'
//import * as React from 'react'
import * as React from 'react'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import { map } from 'lodash'
// Styles
import classnames from 'classnames'

// interface User {
//   name: String
// }
//
// interface UserCollection {
//   items: Array<User>
// }
//
// interface Project {
//   name: String,
//   users: UserCollection
// }
//
// interface ProjectAdminsComponentProps {
//   project: Project,
//   projectAdmins: UserCollection,
//   onAddClick: Function,
//   onConfigureClick: Function,
//   onDeleteClick: Function
// }
//
// interface ProjectAdminsComponentState {
//
// }
//
// class ProjectAdminsComponent extends React.Component<ProjectAdminsComponentProps, ProjectAdminsComponentState> {
//
// }


const ProjectAdminsComponent = ({
  project,
  projectAdmins,
  onAddClick,
  onConfigureClick,
  onDeleteClick,
  styles
}) => {
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
          {map(projectAdmins.items, (projectAdmin, id) => (
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

ProjectAdminsComponent.propTypes = {
  projectAdmins: React.PropTypes.object,
  onAddClick: React.PropTypes.func,
  onConfigureClick: React.PropTypes.func,
  onDeleteClick: React.PropTypes.func
};

export default ProjectAdminsComponent
