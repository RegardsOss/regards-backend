/** @module AdminLayout */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import Drawer from 'material-ui/Drawer'
import MenuItem from 'material-ui/MenuItem'
import PowerSettingsNew from 'material-ui/svg-icons/action/power-settings-new'
import Divider from 'material-ui/Divider';
import Settings from 'material-ui/svg-icons/action/settings'
import People from 'material-ui/svg-icons/social/people'
import Reply from 'material-ui/svg-icons/content/reply'

import { ThemeContextType, ThemeContextInterface } from '../../../../common/theme/ThemeContainerInterface'


/**
 * React Menu component. Display the admin application menu
 */
class MenuComponent extends React.Component<{}, any> {

  context: ThemeContextInterface;
  static contextTypes = ThemeContextType;
  render(){
    const {muiTheme} = this.context;

    return (
      <Drawer open={true}>
        <Link to={"/admin/cdpp/projects"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText="Projets" leftIcon={<Settings />} />
        </Link>

        <Link to={"/admin/cdpp/users"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText="Utilisateurs" leftIcon={<People />} />
        </Link>

        <Divider />

        <MenuItem primaryText="Se déconnecter" leftIcon={<PowerSettingsNew />} />

        <Divider />

        <Link to={"/admin/cdpp"} style={{textDecoration:muiTheme.linkWithoutDecoration.textDecoration}}>
          <MenuItem primaryText="Retour" leftIcon={<Reply />} />
        </Link>

      </Drawer>
    )
  }
}

export default MenuComponent
