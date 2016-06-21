import React, { PropTypes } from 'react'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

const ProjectAdministratorsComponent = React.createClass({
  render () {
    const {
      project,
      onAddClick
    } = this.props

    if(project) {
      return (
        <div>
          Project Administrators
          <i className={icons['fi-plus']} title='Add new administrator' onClick={() => onAddClick(project.id)}></i>
          <br />
          List of administrators for {project.name}:
          <ul>
            {project.admins.map((admin) => (
              <li key={admin.id}>
                {admin.name}
                <i className={icons['fi-wrench']} title='Configure admin user' onClick={() => onAddClick(project.id)}></i>
                <i className={icons['fi-trash']} title='Delete admin user'></i>
              </li>
            ))}
          </ul>
        </div>
      )
    } else {
      return null
    }
  }
})

ProjectAdministratorsComponent.propTypes = {
  project: PropTypes.object
};

export default ProjectAdministratorsComponent
