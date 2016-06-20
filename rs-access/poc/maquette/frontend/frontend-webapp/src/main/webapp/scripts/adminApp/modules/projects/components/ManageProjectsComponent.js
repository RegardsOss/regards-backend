import React, { PropTypes } from 'react'
import { connect } from 'react-redux'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import RegardsSelect from 'common/components/RegardsSelect'

let ManageProjectsComponent = ({
  projects,
  onSelect
}) => {
    return (
      <div>
        <span>Manage projects</span>
        <i className={icons['fi-plus']} title='Add new project'></i>
        <i className={icons['fi-trash']} title='Delete selected project'></i>
        <br />
        <RegardsSelect list={projects.items} label={'Select a project'} onSelect={onSelect}/>
      </div>
    )
}

ManageProjectsComponent.propTypes = {
  projects: PropTypes.object,
  onSelect: PropTypes.func
}

export default ManageProjectsComponent



// <select onChange={onSelect}>
//   <option defaultValue>Select a project...</option>
//   {projects.items.map(project =>
//     <option key={project.id} value={project.id}>{project.name}</option>
//   )}
// </select>
