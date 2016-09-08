/** @module AdminProjects */
import * as React from "react"
import { connect } from "react-redux"
import { map } from "lodash"
import { FormattedMessage } from "react-intl"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import ModuleComponent from "../../../../common/components/ModuleComponent"
import { Card, CardTitle, CardText, CardActions } from "material-ui/Card"
import AddProject from "../components/AddProject"
import * as actions from "../actions"
import * as selectors from "../../../reducer"
import SecondaryActionButtonComponent from "../../../../common/components/SecondaryActionButtonComponent"
import { Table, TableHeader, TableHeaderColumn, TableBody, TableRow, TableRowColumn } from "material-ui/Table"
import { Project } from "../../../../common/models/projects/Project"
import Camera from "material-ui/svg-icons/image/camera"
import Edit from "material-ui/svg-icons/editor/mode-edit"
import Delete from "material-ui/svg-icons/action/delete"
import IconButton from "material-ui/IconButton"
import { browserHistory } from "react-router"

interface ProjectsContainerTypes {
  // From mapStateToProps
  projects: Array<Project>,
  projectId: string,
  // From mapDispatchToProps
  onLoad?: () => void,
  deleteProject?: (id: string) => void,
  createProject?: () => void,
}

/**
 * React container to manage ManageProjectsComponent.
 *
 * @prop {Array<Project>} projects List of projects to display
 * @prop {Boolean} projectConfigurationIsShown ProjectConfigurationComponent display status
 *
 */
class ProjectsContainer extends React.Component<ProjectsContainerTypes, any> {

  state: any = {
    open: false
  }

  componentWillMount (): any {
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad()
  }

  handleSave = () => {
    this.props.createProject()
    // const id = '999'
    // const name = 'Projet test'
    // this.props.addProject(id, name)
  }

  handleDelete = () => {
    console.log("Todo")
    // this.props.deleteProject(this.props.projectId)
  }

  handleAdd = () => {
    this.setState({open: true})
  }

  handleClose = () => {
    this.setState({open: false})
  }

  handleView = (selectedRows: number[] | string) => {
    if(selectedRows instanceof String)
      throw new Error('Only a single row should be selected in the table')
    if(selectedRows instanceof Array && selectedRows.length !== 1)
      throw new Error('Exactly one row is expected to be selected in the table')

    const project = this.props.projects[selectedRows[0]]
    const url = "/admin/" + "cdpp" + "/projects/" + project.projectId
    browserHistory.push(url)
  }

  handleEdit = () => {
    console.log("Todo")
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/projects/i18n'>
        <ModuleComponent>
          <Card>
            <CardTitle
              title={<FormattedMessage id='projects.title'/>}
              subtitle={<FormattedMessage id='projects.subtitle'/>}
            />
            <CardText>
              <Table
                selectable={true}
                onRowSelection={this.handleView} >
                <TableHeader
                  enableSelectAll={false}
                  adjustForCheckbox={false}
                  displaySelectAll={false} >
                  <TableRow>
                    <TableHeaderColumn></TableHeaderColumn>
                    <TableHeaderColumn>Nom</TableHeaderColumn>
                    <TableHeaderColumn>Description</TableHeaderColumn>
                    <TableHeaderColumn>Public</TableHeaderColumn>
                    <TableHeaderColumn>Actions</TableHeaderColumn>
                  </TableRow>
                </TableHeader>
                <TableBody
                  displayRowCheckbox={false}
                  preScanRows={false}
                  showRowHover={true} >
                  {map(this.props.projects, (p: Project, i: number) => (
                    <TableRow key={i}>
                      <TableRowColumn><Camera/></TableRowColumn>
                      <TableRowColumn>{p.name}</TableRowColumn>
                      <TableRowColumn>{p.description}</TableRowColumn>
                      <TableRowColumn>{p.isPublic}</TableRowColumn>
                      <TableRowColumn>
                        <IconButton tooltip="Font Icon">
                          <Edit onTouchTap={this.handleEdit} />
                        </IconButton>
                        <IconButton tooltip="Supprimer">
                          <Delete onTouchTap={this.handleDelete} />
                        </IconButton>
                      </TableRowColumn>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardText>
            <CardActions>
              <SecondaryActionButtonComponent
                label={<FormattedMessage id="projects.delete.button.title"/>}
                onTouchTap={this.handleDelete}
                isVisible={false}
              />
              <AddProject onSave={this.handleSave}/>
            </CardActions>
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
  deleteProject: (id: string) => dispatch(actions.deleteProject(id)),
  createProject: () => dispatch(actions.createProject())
})

export default connect<{}, {}, ProjectsContainerTypes>(mapStateToProps, mapDispatchToProps)(ProjectsContainer)
