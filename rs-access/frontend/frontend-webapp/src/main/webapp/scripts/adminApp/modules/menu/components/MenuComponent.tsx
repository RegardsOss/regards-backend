/** @module AdminLayout */
import * as React from "react"
import { Link } from "react-router"
import Drawer from "material-ui/Drawer"
import MenuItem from "material-ui/MenuItem"
import PowerSettingsNew from "material-ui/svg-icons/action/power-settings-new"
import Divider from "material-ui/Divider"
import Settings from "material-ui/svg-icons/action/settings"
import People from "material-ui/svg-icons/social/people"
import Reply from "material-ui/svg-icons/content/reply"

import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
import { FormattedMessage } from "react-intl"

/**
 * React Menu component. Display the admin application menu
 */
class MenuComponent extends React.Component<{}, any> {
  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  render(): JSX.Element {

    const {muiTheme} = this.context
    const style = muiTheme.linkWithoutDecoration

    return (

      <Drawer
        open={true}
        containerStyle={{width:'100%', height:'100%'}}
        >
        <Link to={"/admin/cdpp/projects"} style={style}>
          <MenuItem primaryText={<FormattedMessage id="menu.projects"/>} leftIcon={<Settings />} />
        </Link>

        <Link to={"/admin/cdpp/users"} style={style}>
          <MenuItem primaryText={<FormattedMessage id="menu.users"/>} leftIcon={<People />}/>
        </Link>

        <Divider />

        <MenuItem primaryText={<FormattedMessage id="menu.logout"/>} leftIcon={<PowerSettingsNew />}/>

        <Divider />

        <Link to={"/admin/cdpp"} style={style}>
          <MenuItem primaryText={<FormattedMessage id="menu.back"/>} leftIcon={<Reply />}/>
        </Link>

      </Drawer>
    )
  }
}

export default MenuComponent
