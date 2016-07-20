/** @module AdminProjects */
import * as React from 'react'
import { PropTypes } from 'react'
import { connect } from 'react-redux'
import { FormattedMessage, intlShape } from 'react-intl'
var icons = require('stylesheets/foundation-icons/foundation-icons.scss')
import RegardsSelect from '../../../../common/components/RegardsSelect'
import { Project } from '../types/ProjectTypes'

interface ManageProjectsType {
  onSelect : ()=> void,
  projects: Array<Project>,
  selectedProjectId: string,
  onAddClick: ()=> void,
  onDeleteClick: (id: string) => void,
  styles : Object
}


/**
 * React component to display a list of projects
 *
 * @prop {Array<Project>} projects          List of projects to display
 * @prop {String}         selectedProjectId Identifier of curent selected project
 * @prop {Object}         styles            CSS Styles
 * @prop {Function}       onSelect          Callback to select a project
 * @prop {Function}       onAddClick        Callback to add a project
 * @prop {Function}       onDeleteClick     Callback to delete a project
 */
class ManageProjectsComponent extends React.Component<ManageProjectsType, any> {
  context: any
  static contextTypes = {
      intl: intlShape
  }
  render(){
    return (
      <div>
        <span><FormattedMessage id='projects.title'/></span>
        <button title={this.context.intl.formatMessage({id:"projects.add.button.title"})} onClick={this.props.onAddClick}>
          <i className={icons['fi-plus']}></i>
        </button>
        <button  title={this.context.intl.formatMessage({id:"projects.delete.button.title"})} onClick={() => this.props.onDeleteClick(this.props.selectedProjectId)}>
          <i className={icons['fi-trash']}></i>
        </button>
        <br/>
        <RegardsSelect
        list={this.props.projects}
        label={this.context.intl.formatMessage({id:"projects.list.select.label"})}
        onSelect={this.props.onSelect}
        displayAttribute="name"
        identityAttribute="id"/>
      </div>
    )
  }
}

export default ManageProjectsComponent
