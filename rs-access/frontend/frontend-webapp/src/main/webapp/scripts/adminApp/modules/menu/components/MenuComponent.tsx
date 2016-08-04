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
import { intlShape } from "react-intl"

import { ThemeContextType } from "../../../../common/theme/ThemeContainerInterface"

/**
 * React Menu component. Display the admin application menu
 */
class MenuComponent extends React.Component<{}, any> {

  static contextTypes: Object = {
    intl: intlShape,
    muiTheme: ThemeContextType.muiTheme
  }
  context: any;


  render(): any {
    const {muiTheme} = this.context

    return (

      <Drawer
        open={true}
        containerStyle={{width:'100%', height:'100%'}}
        >
        <Link to={"/admin/cdpp/projects"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText={this.context.intl.formatMessage({id:"menu.projects"})} leftIcon={<Settings />} />
        </Link>

        <Link to={"/admin/cdpp/users"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText={this.context.intl.formatMessage({id:"menu.users"})} leftIcon={<People />}/>
        </Link>

        <Divider />

        <MenuItem primaryText={this.context.intl.formatMessage({id:"menu.logout"})} leftIcon={<PowerSettingsNew />}/>

        <Divider />

        <Link to={"/admin/cdpp"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText={this.context.intl.formatMessage({id:"menu.back"})} leftIcon={<Reply />}/>
        </Link>

      </Drawer>
    )
  }
}

export default MenuComponent
