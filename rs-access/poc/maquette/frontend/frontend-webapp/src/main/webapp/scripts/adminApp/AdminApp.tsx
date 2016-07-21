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

// Theme
import ThemeHelper from '../common/theme/ThemeHelper'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'

import RaisedButton from 'material-ui/RaisedButton'
import SelectTheme from '../common/theme/containers/SelectTheme'

interface AminAppProps {
  router: any,
  route : any,
  params: any,
  currentTheme: string,
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
    const { currentTheme, authentication, content, location, params, onLogout } = this.props
    const muiTheme = ThemeHelper.getByName(currentTheme)
    // const styles = getThemeStyles(theme, 'adminApp/styles')
    // const commonStyles = getThemeStyles(theme,'common/common.scss')
    if (authentication){
      let authenticated = authentication.authenticateDate + authentication.user.expires_in > Date.now()
      authenticated = authenticated && (authentication.user.name !== undefined) && authentication.user.name !== 'public'
      if (authenticated === false){
        return (
          <MuiThemeProvider muiTheme={muiTheme}>
            <div>
              <Authentication />
            </div>
          </MuiThemeProvider>
        );
      } else {
          return (
            <MuiThemeProvider muiTheme={muiTheme}>
                <Layout>
                  <div key='1' style={{backgroundColor:'#ff4081',height: '100%'}}>
                    <RaisedButton label="Click!" />
                  </div>
                  <div key='2' style={{backgroundColor:'#FFCA28'}}>
                  </div>
                  <div key='3'>
                    <SelectTheme/>
                  </div>
                </Layout>
            </MuiThemeProvider>
          );
      }
    }
    else {
      return <ErrorComponent />
    }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state: any) => ({
  currentTheme: state.common.themes.selected,
  authentication: state.common.authentication
})
const mapDispatchToProps = (dispatch: any) => ({
  onLogout: () => {dispatch(logout())}
})
export default connect<{}, {}, AminAppProps>(mapStateToProps,mapDispatchToProps)(AdminApp)
