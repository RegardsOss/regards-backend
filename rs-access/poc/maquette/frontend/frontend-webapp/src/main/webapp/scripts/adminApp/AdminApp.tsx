/** @module AdminApp */
import * as React from 'react'
import { connect } from 'react-redux'
import * as ReactDOM from 'react-dom'

import { logout } from '../common/authentication/AuthenticateActions'
import { getThemeStyles } from '../common/theme/ThemeUtils'
import Authentication from './modules/authentication/containers/AuthenticationContainer'
import { AuthenticationType } from '../common/authentication/AuthenticationTypes'

import ErrorComponent from '../common/components/ApplicationErrorComponent'
import Layout from '../common/layout/containers/Layout'
import Home from './modules/home/Home'
import MenuComponent from './modules/menu/components/MenuComponent'
// Theme
import ThemeHelper from '../common/theme/ThemeHelper'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import SelectTheme from '../common/theme/containers/SelectTheme'

interface AminAppProps {
  router: any,
  route : any,
  params: any,
  theme: string,
  authentication: AuthenticationType,
  content: any,
  location: any,
  onLogout: ()=> void
}

/**
 * React component to manage Administration application.
 * This component display admin layout or login form if the user is not connected
 */
class AdminApp extends React.Component<AminAppProps, any> {
  constructor(){
    super()
    this.state = { instance: false }
  }

  render(){
    const { theme, authentication, content, location, params, onLogout } = this.props

    // Build theme
    const muiTheme = ThemeHelper.getByName(theme)

    if (authentication){
      let authenticated = authentication.authenticateDate + authentication.user.expires_in > Date.now()
      authenticated = authenticated && (authentication.user.name !== undefined) && authentication.user.name !== 'public'
      if (authenticated === false){
        return (
          <MuiThemeProvider muiTheme={muiTheme}>
            <Authentication />
          </MuiThemeProvider>
        )
      } else {
          return (
            <MuiThemeProvider muiTheme={muiTheme}>
              <div>
                <MenuComponent />
                <Layout>
                  <div key='1'>{content}</div>
                  <div key='2'><SelectTheme/></div>
                </Layout>
              </div>
            </MuiThemeProvider>
          )
      }
    }
    else {
      return (
        <MuiThemeProvider muiTheme={muiTheme}>
          <ErrorComponent />
        </MuiThemeProvider>
      )
    }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state: any) => ({
  theme: state.common.themes.selected,
  authentication: state.common.authentication
})
const mapDispatchToProps = (dispatch: any) => ({
  onLogout: () => {dispatch(logout())}
})
export default connect<{}, {}, AminAppProps>(mapStateToProps,mapDispatchToProps)(AdminApp)
