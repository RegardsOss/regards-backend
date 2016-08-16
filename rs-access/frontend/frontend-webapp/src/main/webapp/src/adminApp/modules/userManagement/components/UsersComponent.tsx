import { Table, TableHeader, TableRow, TableHeaderColumn, TableBody, TableRowColumn } from 'material-ui'
import { RaisedButton } from "material-ui"
import UserComponent from './UserComponent'
import { User } from '../types'
import { FormattedMessage } from "react-intl"

interface UsersProps {
  users: Array<User>,
  onDeleteUser: (user:User)=>void,
  onEditUser: (user:User)=>void,
  onViewUser: (user:User)=>void,
  onCreateUser: ()=> void
}

export default class UsersComponent extends React.Component<UsersProps, any> {

  render(): JSX.Element {
    return (
      <div>
      <RaisedButton label="Create new user" fullWidth={true} onClick={this.props.onCreateUser}/>
      <Table selectable={true}
          multiSelectable={true}>
        <TableHeader>
          <TableRow>
            <TableHeaderColumn><FormattedMessage id="userlist.header.id.label"/></TableHeaderColumn>
            <TableHeaderColumn><FormattedMessage id="userlist.header.login.label"/></TableHeaderColumn>
            <TableHeaderColumn><FormattedMessage id="userlist.header.status.label"/></TableHeaderColumn>
            <TableHeaderColumn><FormattedMessage id="userlist.header.actions.label"/></TableHeaderColumn>
          </TableRow>
        </TableHeader>
        <TableBody
            displayRowCheckbox={true}
            showRowHover={true}
            deselectOnClickaway={false}
            stripedRows={true} >
        {this.props.users.map( (row, index) => (
            <UserComponent key={row.account.accountId}
             user={row}
             onViewUser={this.props.onViewUser}
             onDeleteUser={this.props.onDeleteUser}
             onEditUser={this.props.onEditUser}
             />
            ))}
        </TableBody>
      </Table>
      </div>
    )
  }
}
