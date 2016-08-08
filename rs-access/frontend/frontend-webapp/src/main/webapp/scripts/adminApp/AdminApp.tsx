/** @module AdminApp */
import * as React from "react"
import { connect } from "react-redux"
import { logout } from "../common/authentication/AuthenticateActions"
import Authentication from "./modules/authentication/containers/AuthenticationContainer"
import { AuthenticationType } from "../common/authentication/AuthenticationTypes"
import { isAuthenticated } from "../common/authentication/AuthenticateUtils"
import Layout from "../common/layout/containers/Layout"
import MenuContainer from "./modules/menu/containers/MenuContainer"
import AppBar from "material-ui/AppBar"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import IconButton from "material-ui/IconButton"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import ThemeHelper from "../common/theme/ThemeHelper"
import MuiThemeProvider from "material-ui/styles/MuiThemeProvider"
import SelectTheme from "../common/theme/containers/SelectTheme"
import SelectLanguage from "../common/i18n/containers/SelectLocaleContainer"

interface AminAppProps {
  router: any,
  route: any,
  params: any,
  theme: string,
  authentication: AuthenticationType,
  content: any,
  location: any,
  onLogout: () => void
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
class AdminApp extends React.Component<AminAppProps, any> {
  constructor () {
    super()
    this.state = {instance: false}
  }

  render (): JSX.Element {
    const {theme, authentication, content} = this.props

    // Build theme
    const muiTheme = ThemeHelper.getByName(theme)

    // Authentication
    const authenticated = isAuthenticated(authentication)
    if (authenticated === false) {
      const layoutStyle = Object.assign({}, {display: "flex", alignItems: "center"}, muiTheme.adminApp.loginForm);
      return (
        <MuiThemeProvider muiTheme={muiTheme}>
          <Layout style={layoutStyle}>
            <div key='selectTheme'><SelectTheme /></div>
            <div key='authentication'><Authentication /></div>
            <div key='selectLanguage'><SelectLanguage locales={['en','fr']}/></div>
          </Layout>
        </MuiThemeProvider>
      )
    } else {
      return (
        <MuiThemeProvider muiTheme={muiTheme}>
          <div>
            <Layout>
              <div key='sideBar'><MenuContainer /></div>
              <div key='appBar'><AppBar title="Regards admin dashboard" iconElementRight={AdminAppBarIcon}/></div>
              <div key='content'>{content}</div>
              <div key='selectTheme'><SelectTheme /></div>
              <div key='selectLanguage'><SelectLanguage locales={['en','fr']}/></div>
            </Layout>
          </div>
        </MuiThemeProvider>
      )
    }
  }
}

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
export default connect<{}, {}, AminAppProps>(mapStateToProps, mapDispatchToProps)(AdminApp)
