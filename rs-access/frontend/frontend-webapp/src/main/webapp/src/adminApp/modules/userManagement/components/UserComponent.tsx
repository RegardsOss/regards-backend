import { TableRow, TableRowColumn } from 'material-ui'
import IconButton from "material-ui/IconButton"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import { grey900 } from "material-ui/styles/colors"
import { FormattedMessage } from "react-intl"
import UserActionsComponent from "./UserActionsComponent"
import { User } from "../types"

interface UserInterface {
  user: any,
  onViewUser: (user:User) => void,
  onDeleteUser: (user:User) => void,
  onEditUser: (user:User) => void,
  children?: any
}

export default class UserComponent extends React.Component<UserInterface, any> {

  constructor(){
    super()
    this.editUser = this.editUser.bind(this)
    this.viewUser = this.viewUser.bind(this)
    this.deleteUser = this.deleteUser.bind(this)
  }

  editUser(): void {
    this.props.onEditUser(this.props.user)
  }

  deleteUser(): void {
    this.props.onDeleteUser(this.props.user)
  }

  viewUser(): void {
    this.props.onViewUser(this.props.user)
  }

  render(): JSX.Element {
    console.log("Render user:",this.props.user.account.accountId)
    return (
      <TableRow {...this.props} >
        {
          // Display checkbox
          this.props.children
        }
        <TableRowColumn>{this.props.user.account.accountId}</TableRowColumn>
        <TableRowColumn>{this.props.user.account.login}</TableRowColumn>
        <TableRowColumn><FormattedMessage id={"user.status."+this.props.user.status+".label"} /></TableRowColumn>
        <TableRowColumn>
          <UserActionsComponent
            onDeleteUser={this.deleteUser}
            onEditUser={this.editUser}
            onViewUser={this.viewUser}
          >
          </UserActionsComponent>
        </TableRowColumn>
      </TableRow>
    )
  }
}
