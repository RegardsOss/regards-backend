/** @module ProjectUsers */
import * as React from "react"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { User } from "../../../../common/users/types"
import { ListItem } from "material-ui/List"
import IconPeople from "material-ui/svg-icons/social/people"
import { Link } from "react-router"
import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn } from "material-ui/Table"
import Checkbox from 'material-ui/Checkbox'

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
  render(): JSX.Element {
    const {user, redirectOnSelectTo} = this.props
    const {muiTheme} = this.context
    const style = muiTheme.palette.errorColor
    return (
      <I18nProvider messageDir="adminApp/modules/projectUsers/i18n">
          <TableRow>
            <TableRowColumn>
              <Link to={redirectOnSelectTo} style={style}>
                {user.login}
              </Link>
            </TableRowColumn>
            <TableRowColumn>
              <Link to={redirectOnSelectTo} style={style}>
                <Checkbox
                  label="azertyui"
                />
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
          </TableRow>
      </I18nProvider>
    )
  }
}

export default ProjectUserComponent
