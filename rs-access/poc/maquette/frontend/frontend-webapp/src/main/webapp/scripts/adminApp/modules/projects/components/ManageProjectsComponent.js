import React, { PropTypes } from 'react'
import { connect } from 'react-redux'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import RegardsSelect from 'common/components/RegardsSelect'

let ManageProjectsComponent = ({
  projects,
  onSelect,
  onAddClick,
  onDeleteClick,
}) => {
    return (
      <div>
        <span>Manage projects</span>
        <i className={icons['fi-plus']} title='Add new project' onClick={onAddClick} ></i>
        <i className={icons['fi-trash']} title='Delete selected project' onClick={onDeleteClick}></i>
        <br />
        <RegardsSelect list={projects} label={'Select a project'} onSelect={onSelect}/>
      </div>
    )
}

ManageProjectsComponent.propTypes = {
  projects: PropTypes.array,
  onSelect: PropTypes.func
}

export default ManageProjectsComponent
