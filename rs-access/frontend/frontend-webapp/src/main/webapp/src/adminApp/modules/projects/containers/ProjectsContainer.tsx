/** @module AdminProjects */
import * as React from "react"
import { connect } from "react-redux"
import { map } from "lodash"
import { FormattedMessage } from "react-intl"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { ProjectAdminsContainer } from "../../projectAdmins/index"
import ModuleComponent from "../../../../common/components/ModuleComponent"
import { Card, CardTitle, CardText } from "material-ui/Card"
import RaisedButton from "material-ui/RaisedButton"
import SelectField from "material-ui/SelectField"
import Delete from "material-ui/svg-icons/action/delete"
import MenuItem from "material-ui/MenuItem"
import AddProject from "../components/AddProject"
import { Project } from "../types/ProjectTypes"
import * as actions from "../actions"
import * as uiActions from "../../ui/actions"
import * as selectors from "../../../reducer"
// Containers
// Components
// Types
// Actions
// Selectors

interface ProjectsContainerTypes {
  // From mapStateToProps
  projects: Array<Project>,
  projectId: string,
  // From mapDispatchToProps
  onLoad?: () => void,
  selectProject?: (id: string) => void,
  deleteProject?: (id: string) => void,
  addProject?: (id: string, name: string) => void
}

/**
 * React container to manage ManageProjectsComponent.
 *
 * @prop {Array<Project>} projects List of projects to display
 * @prop {Boolean} projectConfigurationIsShown ProjectConfigurationComponent display status
 *
 */
class ProjectsContainer extends React.Component<ProjectsContainerTypes, any> {

  componentWillMount (): any {
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad()
  }

  handleSave = () => {
    const id = '999'
    const name = 'Fake Project'
    this.props.addProject(id, name)
    this.props.selectProject(id)
  }

  handleDelete = () => {
    this.props.deleteProject(this.props.projectId)
  }

  handleChange = (event: Object, key: number, payload: any) => {
    this.props.selectProject(payload)
  }

  render (): JSX.Element {
    const cardTitle = (
      <div>
        <span style={{float:'left'}}>
           <FormattedMessage id='projects.legend'/>
        </span>
        <AddProject onSave={this.handleSave}/>
      </div>
    )

    return (
      <I18nProvider messageDir='adminApp/modules/projects/i18n'>
        <ModuleComponent>
          <Card>
            <CardTitle title={cardTitle}/>
            <CardText>
              <SelectField
                value={this.props.projectId}
                onChange={this.handleChange}
                floatingLabelText="Select a project">
                {this.props.projects.map((project) => {
                  return <MenuItem key={project.id} value={project.id} primaryText={project.name}/>
                })}
              </SelectField>
              <RaisedButton
                label="Delete"
                labelPosition="before"
                secondary={true}
                icon={<Delete />}
                onClick={this.handleDelete}/>
              <ProjectAdminsContainer />
            </CardText>
          </Card>
        </ModuleComponent>
      </I18nProvider>
    )
  }
}

const mapStateToProps = (state: any) => ({
  projects: map(selectors.getProjects(state).items, (value: any, key: string) => ({id: key, name: value.name})),
  projectId: selectors.getSelectedProjectId(state)
})
const mapDispatchToProps = (dispatch: any) => ({
  onLoad: () => dispatch(actions.fetchProjects()),
  selectProject: (id: string) => dispatch(uiActions.selectProject(id)),
  deleteProject: (id: string) => dispatch(actions.deleteProject(id)),
  addProject: (id: string, name: string) => dispatch(actions.addProject(id, name))
})

export default connect<{}, {}, ProjectsContainerTypes>(mapStateToProps, mapDispatchToProps)(ProjectsContainer)
