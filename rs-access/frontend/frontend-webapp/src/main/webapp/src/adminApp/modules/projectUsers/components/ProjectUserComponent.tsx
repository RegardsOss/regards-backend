/** @module ProjectUsers */
import * as React from "react"
import { User } from "../../../../common/users/types"
import { Link } from "react-router"
import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
import { TableRowColumn, TableRow } from "material-ui/Table"
import { grey900 } from "material-ui/styles/colors"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import IconButton from "material-ui/IconButton"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import { FormattedMessage } from "react-intl"
// Containers
/**
 *
 */
export interface ProjectUserProps {
  user: User
  redirectOnSelectTo: string
  muiTheme?: any
  handleDelete: () => void
  handleView: () => void
  handleEdit: () => void
}


/**
 * React component
 */
class ProjectUserComponent extends React.Component<ProjectUserProps, any> {
  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface


  handleView = () => {
  }

  handleEdit = () => {
  }

  handleDelete = () => {
    this.props.handleDelete()
  }

  /**
   *
   * @returns {any}
   */
  render (): JSX.Element {
    const {user, redirectOnSelectTo} = this.props
    const {muiTheme} = this.context
    const style = muiTheme.linkWithoutDecoration
    return (
      <TableRow>
        <TableRowColumn>
          {this.props.children}
          <Link to={redirectOnSelectTo} style={style}>
            {user.login}
          </Link>
        </TableRowColumn>
        < TableRowColumn >
          <Link to={redirectOnSelectTo} style={style}>
            {user.firstName}
          </Link>
        </ TableRowColumn >
        <TableRowColumn>
          <Link to={redirectOnSelectTo} style={style}>
            {user.lastName}
          </Link>
        </TableRowColumn>
        < TableRowColumn >
          <Link to={redirectOnSelectTo} style={style}>
            {user.email}
          </Link>
        </ TableRowColumn >
        <TableRowColumn>
          <Link to={redirectOnSelectTo} style={style}>
            {user.status}
          </Link>
        </TableRowColumn>
        < TableRowColumn >
          <IconMenu
            iconButtonElement={
                  <IconButton touch={true}>
                    {/*Todo: Extract color to the theme*/}
                    <MoreVertIcon color={grey900}/>
                  </IconButton>
                }
            anchorOrigin={{horizontal: 'left', vertical: 'top'}}
            targetOrigin={{horizontal: 'left', vertical: 'top'}}
          >
            <MenuItem onTouchTap={this.props.handleDelete} primaryText={<FormattedMessage id="dropdown.delete"/>}/>
            <MenuItem onTouchTap={this.props.handleView} primaryText={<FormattedMessage id="dropdown.view"/>}/>
            <MenuItem onTouchTap={this.props.handleEdit} primaryText={<FormattedMessage id="dropdown.edit"/>}/>
          </IconMenu>

        </ TableRowColumn>
      </TableRow>
    )
  }
}

export default ProjectUserComponent
