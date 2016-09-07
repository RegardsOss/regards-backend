import * as React from "react"
import Paper from 'material-ui/Paper'
import AppBar from 'material-ui/AppBar'
// Icons
import IconButton from 'material-ui/IconButton'
import ArrowBack from 'material-ui/svg-icons/navigation/arrow-back'
import ThemeInjector from "../../../../common/theme/ThemeInjector"
import { browserHistory } from "react-router"
import { Project } from "../../../../common/models/projects/types"
import Subheader from "material-ui/Subheader"
import { List, ListItem } from "material-ui/List"
import Divider from "material-ui/Divider"

const styles = {
  headline: {
    fontSize: 24,
    paddingTop: 16,
    marginBottom: 12,
    fontWeight: 400,
  },
  root: {
    display: 'flex',
    flexWrap: 'wrap',
  }
}

interface ProjectReadProps {
  project: Project
  theme: any
}

class ProjectReadComponent extends React.Component<ProjectReadProps, any> {

  handleEdit = () => {
    console.log('todo')
  }

  handleDelete = () => {
    console.log('todo')
  }

  handleBackClick = () => {
    browserHistory.goBack()
  }

  render (): JSX.Element {
    return (
      <Paper>
        <AppBar
          style={{backgroundColor:this.props.theme.palette.accent1Color}}
          title="CDPP"
          iconElementLeft={<IconButton onTouchTap={this.handleBackClick}><ArrowBack /></IconButton>}
        />
        <List>
          <Subheader>Description</Subheader>
          <ListItem
            disabled={true}
            primaryText={"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut sed nunc vulputate, egestas elit non, vulputate sem. Duis elementum iaculis magna vel vulputate. Sed convallis, nisl et luctus dapibus, elit enim vehicula nulla, consectetur sodales purus tellus non justo. Cras non eleifend turpis. Nunc rutrum aliquam dapibus. Praesent volutpat lacus nec tristique sodales. Suspendisse sodales sollicitudin elit, vel sagittis nisl ultrices at. Morbi ac viverra massa."} />
          <Divider />
        </List>
      </Paper>
    )
  }
}

export default ProjectReadComponent

export class ThemedProjectReadComponent extends React.Component<any, any> {
  render(): JSX.Element {
    return (
      <ThemeInjector>
        <ProjectReadComponent theme={null} project={null}/>
      </ThemeInjector>
    )
  }
}
