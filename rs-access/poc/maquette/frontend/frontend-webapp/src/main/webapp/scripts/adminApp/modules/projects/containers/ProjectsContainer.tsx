/** @module AdminProjects */
import * as React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'
var classnames = require('classnames')
import { map } from 'lodash'
// Containers
import { ProjectAdminsContainer } from '../../projectAdmins'
// Components
import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import ModuleComponent from '../../../../common/components/ModuleComponent'
import {Card, CardActions, CardHeader, CardMedia, CardTitle, CardText} from 'material-ui/Card'
import FlatButton from 'material-ui/FlatButton'
import RaisedButton from 'material-ui/RaisedButton'
import SelectField from 'material-ui/SelectField';
import AddBox from 'material-ui/svg-icons/content/add-box'
import Delete from 'material-ui/svg-icons/action/delete'
import Menu from 'material-ui/Menu';
import MenuItem from 'material-ui/MenuItem'
import AddProject from '../components/AddProject';
import IconButton from 'material-ui/IconButton'
// Types
import { Project } from '../types/ProjectTypes'
// Actions
import * as actions from '../actions'
import * as uiActions from '../../ui/actions'
// Selectors
import * as selectors from '../../../reducer'

interface ProjectsContainerTypes {
  projects: Array<Project>,
  projectId: string,
  project?: Project,
  projectConfigurationIsShown: boolean,
  styles : any,
  // Parameters set by react-redux connection
  onLoad? : () => void,
  hideProjectConfiguration? : () => void,
  showProjectConfiguration?: () => void,
  onSave?: () => void,
  selectProject? : (id: string) => void,
  deleteProject? : (id: string) => void
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
    const cardTitle = (
      <div>
        <span style={{float:'left'}}>Projects</span>
        <AddProject onSave={this.props.onSave}/>
      </div>
    )

    return (
      <ModuleComponent>
        <Card>
          <CardTitle title={cardTitle} />
          <CardText>
            <SelectField
              value={this.props.projectId}
              onChange={(e, index, value) => this.props.selectProject(value)}
              floatingLabelText="Select a project" >
              {this.props.projects.map((project) => {
                return <MenuItem key={project.id} value={project.id} primaryText={project.name} />
              })}
            </SelectField>
            <RaisedButton
              label="Delete"
              labelPosition="before"
              secondary={true}
              icon={<Delete />}
              onClick={() => this.props.deleteProject(this.props.projectId)} />
            <AccessRightsComponent dependencies={null}>
              <ProjectAdminsContainer />
            </AccessRightsComponent>
          </CardText>
        </Card>
      </ModuleComponent>
    )
  }
}

// <ProjectConfigurationComponent
//   show={this.props.projectConfigurationIsShown}
//   handleSubmit={this.props.onSave}
//   onCancelClick={this.props.hideProjectConfiguration} />

// <IconButton
// onClick={() => this.props.deleteProject(this.props.projectId)} >
// <Delete />
// </IconButton>

// <FlatButton
//   label="Add"
//   labelPosition="before"
//   primary={true}
//   icon={<AddBox />}
//   onClick={this.props.showProjectConfiguration} />

const mapStateToProps = (state: any) => ({
  projects: map(selectors.getProjects(state).items, (value: any, key: string) => ({id:key, name:value.name})),
  projectId: selectors.getSelectedProjectId(state),
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
  styles: getThemeStyles(state.common.theme, 'adminApp/styles')
})
const mapDispatchToProps = (dispatch: any) => ({
  onLoad:                   () => dispatch(actions.fetchProjects()),
  selectProject:            (id: string) => dispatch(uiActions.selectProject(id)),
  deleteProject:            (id: string) => dispatch(actions.deleteProject(id)),
  showProjectConfiguration: () => dispatch(uiActions.showProjectConfiguration()),
  hideProjectConfiguration: () => dispatch(uiActions.hideProjectConfiguration()),
  onSave:                   (name: string) => {
    let id = ''+Math.floor(100000*Math.random()) // TODO
    dispatch(actions.addProject(id, name))
    dispatch(uiActions.hideProjectConfiguration())
    dispatch(uiActions.selectProject(id))
  }
})

export default connect<{}, {}, ProjectsContainerTypes>(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
