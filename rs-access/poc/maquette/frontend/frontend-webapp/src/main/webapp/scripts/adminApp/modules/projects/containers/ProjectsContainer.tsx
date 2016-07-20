/** @module AdminProjects */
import * as React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'
var classnames = require('classnames')
import { map } from 'lodash'
import { FormattedMessage, intlShape } from 'react-intl'
// Containers
import I18nProvider from '../../../../common/i18n/I18nProvider'
import { ProjectAdminsContainer, UserFormContainer } from '../../projectAdmins'
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import ModuleComponent from '../../../../common/components/ModuleComponent'
// Types
import { Project } from '../types/ProjectTypes'
// Actions
import * as actions from '../actions'
import * as uiActions from '../../ui/actions'
// Selectors
import * as selectors from '../../../reducer'

interface ProjectsContainerTypes {
  projects: Array<Project>,
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

/**
 * React container to manage ManageProjectsComponent.
 *
 * @prop {Array<Project>} projects List of projects to display
 * @prop {Boolean} projectConfigurationIsShown ProjectConfigurationComponent display status
 * @prop {Object} styles CSS Styles
 *
 */
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
      <I18nProvider messageDir='adminApp/modules/projects/i18n'>
        <ModuleComponent>
          <fieldset className={className}>

            <legend>
              <FormattedMessage id='projects.legend'/>
            </legend>

            <ProjectConfigurationComponent
              styles={this.props.styles}
              show={this.props.projectConfigurationIsShown}
              handleSubmit={this.props.handleSubmit}
              onCancelClick={this.props.hideProjectConfiguration} />

            <ManageProjectsComponent
              styles={this.props.styles}
              projects={this.props.projects}
              selectedProjectId={this.props.selectedProjectId}
              onSelect={this.props.onSelect}
              onAddClick={this.props.showProjectConfiguration}
              onDeleteClick={this.props.deleteProject} />

            <AccessRightsComponent dependencies={null}>
              <ProjectAdminsContainer/>
            </AccessRightsComponent>

          </fieldset>
        </ModuleComponent>
      </I18nProvider>
    )
  }
}

const mapStateToProps = (state: any) => ({
  projects: map(selectors.getProjects(state).items, (value: any, key: string) => ({id:key, name:value.name})),
  selectedProjectId: selectors.getSelectedProjectId(state),
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
  styles: getThemeStyles(state.common.theme, 'adminApp/styles')
})
const mapDispatchToProps = (dispatch: any) => ({
  onLoad:                   ()  => dispatch(actions.fetchProjects()),
  onSelect:                 (e: any) => dispatch(uiActions.selectProject(e.target.value)),
  deleteProject:            (id: string)=> dispatch(actions.deleteProject(id)),
  showProjectConfiguration: ()  => dispatch(uiActions.showProjectConfiguration()),
  hideProjectConfiguration: ()  => dispatch(uiActions.hideProjectConfiguration()),
  handleSubmit:             (e: any) => {
    let idProject = "9999" // TODO
    dispatch(actions.addProject(idProject, e.projectName))
    dispatch(uiActions.hideProjectConfiguration())
    dispatch(uiActions.selectProject(idProject))
  }
})

export default connect<{}, {}, ProjectsContainerTypes>(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
