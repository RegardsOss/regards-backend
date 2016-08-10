/** @module AdminMenu */
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
import { map } from 'lodash'
import MenuElement from './MenuElement'
import { HateoasControlledMenuElement } from './MenuElement'
import I18nProvider from '../../../../common/i18n/I18nProvider'
import { FormattedMessage } from "react-intl"

/**
 * React Menu component. Display the admin application menu
 */
class MenuComponent extends React.Component<any, any> {

  static contextTypes: Object = {
    intl: intlShape,
    muiTheme: ThemeContextType.muiTheme
  }
  context: any

  render(): JSX.Element {
    const { muiTheme } = this.context
    const { endpoints } = this.props
    const style = muiTheme.linkWithoutDecoration

    return (
        <Drawer open={true} containerStyle={{width:'100%', height:'100%'}} >
          <HateoasControlledMenuElement
            endpointKey='projects_url'
            key='0'
            to={"/admin/cdpp/projects"}
            linkStyle={style}
            primaryText={<FormattedMessage id="menu.projects"/>}
            leftIcon={<Settings />}
          />
          <HateoasControlledMenuElement
            endpointKey='projects_users_url'
            key='1'
            to={"/admin/cdpp/users"}
            linkStyle={style}
            primaryText={<FormattedMessage id="menu.users"/>}
            leftIcon={<People />}
          />
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
