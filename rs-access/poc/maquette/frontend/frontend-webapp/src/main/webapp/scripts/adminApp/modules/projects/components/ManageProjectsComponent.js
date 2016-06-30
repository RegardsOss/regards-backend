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
      <div className={styles.row}>
        <span>Manage projects</span>
        <button
          className={styles.button + " " + styles.success}
          onClick={onAddClick} >
          <i className={icons['fi-plus']} title='Add new project'></i>
        </button>
        <button
          className={styles.button + " " + styles.alert}
          onClick={() => onDeleteClick(selectedProjectId)} >
          <i className={icons['fi-trash']} title='Delete selected project' ></i>
        </button>
        <br />
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
