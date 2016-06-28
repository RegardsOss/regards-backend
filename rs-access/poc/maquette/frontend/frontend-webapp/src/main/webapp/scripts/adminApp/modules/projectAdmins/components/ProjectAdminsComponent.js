import React, { PropTypes } from 'react'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

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
        <i className={icons['fi-plus']} title='Add new administrator' onClick={() => onAddClick(project.id)}></i>
        <br />
        List of administrators for {project.name}:
        <ul>
          {projectAdmins.map((admin) => (
            <li key={admin.id}>
              {admin.name}
              <i className={icons['fi-wrench']} title='Configure admin user' onClick={() => onConfigureClick(admin.id)}></i>
              <i className={icons['fi-trash']} title='Delete admin user'  onClick={() => onDeleteClick(admin.id)}></i>
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
  projectAdmins: PropTypes.array,
  onAddClick: PropTypes.func,
  onConfigureClick: PropTypes.func,
  onDeleteClick: PropTypes.func
};

export default ProjectAdminsComponent
