/** @module ProjectUsers */
import * as React from "react"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { User } from "../../../../common/users/types"
import { Link } from "react-router"
import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
import { TableRow, TableRowColumn } from "material-ui/Table"
import Checkbox from "material-ui/Checkbox"
import { grey400 } from "material-ui/styles/colors"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import IconButton from "material-ui/IconButton"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import { FormattedMessage } from "react-intl"
// Containers

export interface ProjectUserProps {
  user: User,
  redirectOnSelectTo: string,
  muiTheme?: any
}


/**
 * React component
 */
class ProjectUserComponent extends React.Component<ProjectUserProps, any> {
  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  render (): JSX.Element {
    const {user, redirectOnSelectTo} = this.props
    const {muiTheme} = this.context
    const style = muiTheme.linkWithoutDecoration
    return (
      <I18nProvider messageDir="adminApp/modules/projectUsers/i18n">
        <TableRow>
          <TableRowColumn>
            <Checkbox/>
          </TableRowColumn>
          <TableRowColumn>
            <Link to={redirectOnSelectTo} style={style}>
              {user.login}
            </Link>
          </TableRowColumn>
          <TableRowColumn>
            <Link to={redirectOnSelectTo} style={style}>
              {user.firstName}
            </Link>
          </TableRowColumn>
          <TableRowColumn>
            <Link to={redirectOnSelectTo} style={style}>
              {user.lastName}
            </Link>
          </TableRowColumn>
          <TableRowColumn>
            <Link to={redirectOnSelectTo} style={style}>
              {user.email}
            </Link>
          </TableRowColumn>
          <TableRowColumn>
            <Link to={redirectOnSelectTo} style={style}>
              {user.status}
            </Link>
          </TableRowColumn>
          <TableRowColumn>

            <IconMenu
              iconButtonElement={
                <IconButton touch={true}>
                  <MoreVertIcon color={grey400}/>
                </IconButton>
              }
              anchorOrigin={{horizontal: 'left', vertical: 'top'}}
              targetOrigin={{horizontal: 'left', vertical: 'top'}}
            >
              <MenuItem primaryText={<FormattedMessage id="dropdown.delete"/>}/>
              <MenuItem primaryText={<FormattedMessage id="dropdown.view"/>}/>
              <MenuItem primaryText={<FormattedMessage id="dropdown.edit"/>}/>
            </IconMenu>

          </TableRowColumn>

        </TableRow>
      </I18nProvider>
    )
  }
}

export default ProjectUserComponent
