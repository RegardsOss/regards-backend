import * as React from 'react'
import { PropTypes } from 'react'
import { connect } from 'react-redux'
var icons = require('stylesheets/foundation-icons/foundation-icons.scss')
import RegardsSelect from '../../../../common/components/RegardsSelect'

interface ManageProjectsType {
  onSelect : ()=> void,
  projects: Array<any>,
  selectedProjectId: string,
  onAddClick: ()=> void,
  onDeleteClick: (id: string) => void,
  styles : any
}

class ManageProjectsComponent extends React.Component<ManageProjectsType, any> {
  render(){
    return (
      <div>
        <span>Manage projects</span>
        <button title='Add new project' onClick={this.props.onAddClick}>
          <i className={icons['fi-plus']}></i>
        </button>
        <button  title='Delete selected project' onClick={() => this.props.onDeleteClick(this.props.selectedProjectId)}>
          <i className={icons['fi-trash']}></i>
        </button>
        <br/>
        <RegardsSelect
        list={this.props.projects}
        label={'Select a project'}
        onSelect={this.props.onSelect}
        displayAttribute="name"
        identityAttribute="id"/>
      </div>
    )
  }
}

export default ManageProjectsComponent
