/** @module AdminApp */
import * as React from "react"
import { connect } from "react-redux"
import { logout } from "../common/authentication/AuthenticateActions"
import Layout from "../common/layout/containers/Layout"
import MenuContainer from "./modules/menu/containers/MenuContainer"
import AppBar from "material-ui/AppBar"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import IconButton from "material-ui/IconButton"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import SelectTheme from "../common/theme/containers/SelectTheme"
import SelectLanguage from "../common/i18n/containers/SelectLocaleContainer"
import { ThemeContextInterface, ThemeContextType } from "../common/theme/ThemeContainerInterface"

interface MainAdminLayoutProps {
  theme?: string,
  authentication?: any,
  content: any,
  location: any,
  onLogout?: () => void
}

const AdminAppBarIcon = (
  <div>
    <IconMenu
      iconButtonElement={<IconButton><MoreVertIcon /></IconButton>}
      anchorOrigin={{horizontal: 'left', vertical: 'top'}}
      targetOrigin={{horizontal: 'left', vertical: 'top'}}
    >
      <MenuItem primaryText="Refresh"/>
      <MenuItem primaryText="Send feedback"/>
      <MenuItem primaryText="Settings"/>
      <MenuItem primaryText="Help"/>
      <MenuItem primaryText="Sign out"/>
    </IconMenu>
  </div>
)

/**
 * React component to manage Administration application.
 * This component display admin layout or login form if the user is not connected
 */
class MainAdminLayout extends React.Component<MainAdminLayoutProps, any> {

  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface
  constructor () {
    super()
    this.state = {instance: false}
  }


  render (): JSX.Element {
    const {content} = this.props

    const layoutStyle = this.context.muiTheme.adminApp.layout
    return (
      <Layout style={layoutStyle}>
        <div key='sideBar'><MenuContainer /></div>
        <div key='appBar'><AppBar title="Regards admin dashboard" iconElementRight={AdminAppBarIcon}/></div>
        <div key='content'>{content}</div>
        <div key='selectTheme'><SelectTheme /></div>
        <div key='selectLanguage'><SelectLanguage locales={['en','fr']}/></div>
      </Layout>
    )
  }
}
export default MainAdminLayout
/*
// Add theme from store to the component props
const mapStateToProps = (state: any) => ({
  theme: state.common.theme,
  authentication: state.common.authentication
})
const mapDispatchToProps = (dispatch: any) => ({
  onLogout: () => {
    dispatch(logout())
  }
})
export default connect<{}, {}, MainAdminLayoutProps>(mapStateToProps, mapDispatchToProps)(MainAdminLayout)
*/
