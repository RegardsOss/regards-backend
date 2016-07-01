//import { React, PropTypes } from 'react'
//import * as React from 'react'
import * as React from 'react'
// import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import { map } from 'lodash'

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
  onDeleteClick
}) => {
  if(project) {
    return (
      <div>
        Project Administrators
        <i  title='Add new administrator' onClick={() => onAddClick(project.id)}></i>
        <br />
        List of administrators for {project.name}:
        <ul>
          {map(projectAdmins.items, (projectAdmin, id) => (
            <li key={id}>
              {projectAdmin.name}
              <i  title='Configure admin user' onClick={() => onConfigureClick(id)}></i>
              <i  title='Delete admin user' onClick={() => onDeleteClick(id)}></i>
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
