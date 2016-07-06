import React, { PropTypes } from 'react'
import { connect } from 'react-redux'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import RegardsSelect from 'common/components/RegardsSelect'

let ManageProjectsComponent = ({
  onSelect,
  projects,
  selectedProjectId,
  onAddClick,
  onDeleteClick,
  styles
}) => {
    return (
      <div>
        <span>Manage projects</span>
        <button title='Add new project' onClick={onAddClick}>
          <i className={icons['fi-plus']} ></i>
        </button>
        <button  title='Delete selected project' onClick={() => onDeleteClick(selectedProjectId)}>
          <i className={icons['fi-trash']}></i>
        </button>
        <br/>
        <RegardsSelect list={projects} label={'Select a project'} onSelect={onSelect}/>
      </div>
    )
}

ManageProjectsComponent.propTypes = {
  projects: PropTypes.array,
  onSelect: PropTypes.func,
  selectedProjectId: PropTypes.string
}

export default ManageProjectsComponent
