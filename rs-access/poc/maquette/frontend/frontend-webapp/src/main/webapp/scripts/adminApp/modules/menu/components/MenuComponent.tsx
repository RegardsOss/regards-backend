/** @module AdminLayout */
import * as React from 'react'
import { connect } from 'react-redux'
import { IndexLink, Link } from 'react-router'
import Drawer from 'material-ui/Drawer'
import MenuItem from 'material-ui/MenuItem'
import IconButton from 'material-ui/IconButton'
import PowerSettingsNew from 'material-ui/svg-icons/action/power-settings-new'
import Divider from 'material-ui/Divider';
import Settings from 'material-ui/svg-icons/action/settings'
import People from 'material-ui/svg-icons/social/people'
import Reply from 'material-ui/svg-icons/content/reply'

/**
 * React Menu component. Display the admin application menu
 */
class MenuComponent extends React.Component<{}, any> {

  render(){

    return (
      <Drawer open={true}>
        <Link to={"/admin/cdpp/projects"}>
          <MenuItem primaryText="Projets" leftIcon={<Settings />} />
        </Link>

        <Link to={"/admin/cdpp/users"}>
          <MenuItem primaryText="Utilisateurs" leftIcon={<People />} />
        </Link>

        <Divider />

        <MenuItem primaryText="Se déconnecter" leftIcon={<PowerSettingsNew />} />

        <Divider />

        <Link to={"/admin/cdpp"}>
          <MenuItem primaryText="Retour" leftIcon={<Reply />} />
        </Link>

      </Drawer>
    )
  }
}

export default MenuComponent
