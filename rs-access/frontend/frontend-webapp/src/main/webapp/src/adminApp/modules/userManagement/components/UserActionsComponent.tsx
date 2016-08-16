import { TableRow, TableRowColumn } from 'material-ui'
import IconButton from "material-ui/IconButton"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import { grey900 } from "material-ui/styles/colors"
import { FormattedMessage } from "react-intl"
import { User } from "../types"

interface UserInterface {
  onViewUser: () => void,
  onDeleteUser:() => void,
  onEditUser:() => void
}

class UserActionsComponent extends React.Component<UserInterface, any> {

  render(): JSX.Element {

    return (
      <IconMenu iconButtonElement={
            <IconButton touch={true}>
              {/*Todo: Extract color to the theme*/}
              <MoreVertIcon color={grey900}/>
              </IconButton>
            }
            anchorOrigin={{horizontal: 'left', vertical: 'top'}}
            targetOrigin={{horizontal: 'left', vertical: 'top'}}>

            <MenuItem onTouchTap={this.props.onDeleteUser} primaryText={<FormattedMessage id="dropdown.delete"/>}/>
            <MenuItem onTouchTap={this.props.onViewUser} primaryText={<FormattedMessage id="dropdown.view"/>}/>
            <MenuItem onTouchTap={this.props.onEditUser} primaryText={<FormattedMessage id="dropdown.edit"/>}/>

      </IconMenu>
    )
  }
}

export default UserActionsComponent
