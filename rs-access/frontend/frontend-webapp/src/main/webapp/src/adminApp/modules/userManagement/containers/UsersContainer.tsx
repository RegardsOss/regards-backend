import * as React from "react"
import { connect } from "react-redux"
import UsersComponent from "../components/UsersComponent"
import { fetchUsers, deleteUser } from '../actions'
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { User } from "../types"
import UserDeleteComponent from "../components/UserDeleteComponent"
import { browserHistory } from "react-router"
import { RaisedButton } from 'material-ui/RaisedButton';

export class UsersContainer extends React.Component<any, any> {

  constructor() {
    super ()
    this.state = {
      openDeleteDialog: false
    }

    this.onDeleteUser = this.onDeleteUser.bind(this)
    this.onViewUser = this.onViewUser.bind(this)
    this.onEditUser = this.onEditUser.bind(this)
    this.handleDeleteUserDialog = this.handleDeleteUserDialog.bind(this)
    this.handleCloseDeleteDialog = this.handleCloseDeleteDialog.bind(this)
    this.onCreateUser = this.onCreateUser.bind(this)
  }

  componentWillMount() : any{
    // Load users
    this.props.loadUsers();
  }

  onDeleteUser(user:User): void{
    console.log("Deleting user",user)
    this.setState({
      openDeleteDialog: true
    })
  }

  onEditUser(user:User): void{
    console.log("Editing User",user)
    const urlTo = "/admin/" + this.props.params.project + "/users/" + user.account.accountId + "/edit";
    browserHistory.push(urlTo)
  }

  onViewUser(user:User): void{
    console.log("Viewing User",user)
    const urlTo = "/admin/" + this.props.params.project + "/users/" + user.account.accountId;
    browserHistory.push(urlTo)
  }

  onCreateUser():void{
    console.log("Creating User")
    const urlTo = "/admin/" + this.props.params.project + "/user/create";
    browserHistory.push(urlTo)
  }

  handleDeleteUserDialog():void {
    this.props.deleteUser()
    this.handleCloseDeleteDialog()
  }

  handleCloseDeleteDialog():void {
    this.setState({
      openDeleteDialog: false
    })
  }

  render(): JSX.Element {
    if (this.props.users && this.props.users.items && this.props.users.items.length > 0){
      let deleteModal:JSX.Element = null;
      if (this.state.openDeleteDialog) {
        deleteModal = <UserDeleteComponent
          onClose={this.handleCloseDeleteDialog}
          onDelete={this.handleDeleteUserDialog}
        />
      }
      return (
        <I18nProvider messageDir="adminApp/modules/userManagement/i18n">
          <div>
            <UsersComponent users={this.props.users.items}
            onDeleteUser={this.onDeleteUser}
            onViewUser={this.onViewUser}
            onEditUser={this.onEditUser}
            onCreateUser={this.onCreateUser}/>
            {deleteModal}

          </div>
        </I18nProvider>
      )
    } else {
      return (<div>Loading users ... </div>)
    }
  }
}

const mapStateToProps = (state: any) => ({
  users: state.adminApp.users
})
const mapDispatchToProps = (dispatch: any) => ({
  loadUsers: () => dispatch (fetchUsers()),
  deleteUser: (user:User) => dispatch(deleteUser(user))
})

export default connect<{}, {}, any> (mapStateToProps, mapDispatchToProps) (UsersContainer)
