import * as React from 'react'
import { Component, PropTypes } from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'
var classnames = require('classnames')
import { map } from 'lodash'
// Containers
import { ProjectAdminsContainer, UserFormContainer } from '../../projectAdmins'
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import ModuleComponent from '../../../../common/components/ModuleComponent'
// Actions
import {
  addProject,
  deleteProject,
  fetchProjects,
  deleteSelectedProject } from '../actions'
import {
  selectProject,
  showProjectConfiguration,
  hideProjectConfiguration,
  showProjectAdminConfiguration } from '../../ui/actions'
// Selectors
import {
  getProjects,
  getSelectedProjectId,
  getProjectById } from '../../../reducer'

interface ProjectsContainerTypes {
  projects: Array<any>,
  selectedProject: any,
  projectConfigurationIsShown: boolean,
  styles : any,
  // Parameters set by react-redux connection
  onLoad? : () => void,
  hideProjectConfiguration? : () => void,
  showProjectConfiguration?: () => void,
  handleSubmit?: () => void,
  onSelect? : () => void,
  selectedProjectId? : string,
  deleteProject? : () => void
}

class ProjectsContainer extends React.Component<ProjectsContainerTypes, any> {
  componentWillMount(){
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad()
  }

  render () {
    const className = classnames(
      this.props.styles['columns'],
      this.props.styles['small-4'],
      this.props.styles['callout'],
      this.props.styles['custom-callout']
    )

    return (
      <ModuleComponent>
        <fieldset className={className}>

          <legend>Projects</legend>

          <ProjectConfigurationComponent
            styles={this.props.styles}
            show={this.props.projectConfigurationIsShown}
            onSubmit={this.props.handleSubmit}
            onCancelClick={this.props.hideProjectConfiguration}
            styles={this.props.styles} />

          <ManageProjectsComponent
            styles={this.props.styles}
            projects={this.props.projects}
            selectedProjectId={this.props.selectedProjectId}
            onSelect={this.props.onSelect}
            onAddClick={this.props.showProjectConfiguration}
            onDeleteClick={this.props.deleteProject}
            styles={this.props.styles} />

          <AccessRightsComponent dependencies={null}>
            <ProjectAdminsContainer/>
          </AccessRightsComponent>

        </fieldset>
      </ModuleComponent>
    )
  }
}

const mapStateToProps = (state: any) => ({
  projects: map(getProjects(state).items, (value: any, key: string) => ({id:key, name:value.name})),
  selectedProjectId: getSelectedProjectId(state),
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
  styles: getThemeStyles(state.common.theme, 'adminApp/styles')
})
const mapDispatchToProps = (dispatch: any) => ({
  onLoad:                   ()  => dispatch(fetchProjects()),
  onSelect:                 (e: any) => dispatch(selectProject(e.target.value)),
  deleteProject:            (id: string)=> dispatch(deleteProject(id)),
  showProjectConfiguration: ()  => dispatch(showProjectConfiguration()),
  hideProjectConfiguration: ()  => dispatch(hideProjectConfiguration()),
  handleSubmit:             (e: any) => {
    let idProject = "9999" // TODO
    dispatch(addProject(idProject, e.projectName))
    dispatch(hideProjectConfiguration())
    dispatch(selectProject(idProject))
  }
})

export default connect<{}, {}, ProjectsContainerTypes>(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
