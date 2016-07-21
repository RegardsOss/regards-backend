/** @module AdminApp */
import * as React from 'react'
import { connect } from 'react-redux'
import * as ReactDOM from 'react-dom'

import { setTheme } from '../common/theme/actions/ThemeActions'
import { logout } from '../common/authentication/AuthenticateActions'
import { getThemeStyles } from '../common/theme/ThemeUtils'
import Authentication from './modules/authentication/containers/AuthenticationContainer'
import { AuthenticationType } from '../common/authentication/AuthenticationTypes'
import SelectThemeComponent from '../common/theme/components/SelectThemeComponent'
import SelectLocaleComponent from '../common/i18n/SelectLocaleComponent'
import ErrorComponent from '../common/components/ApplicationErrorComponent'
import Layout from './modules/layout/Layout'

interface AminAppProps {
  router: any,
  route : any,
  params: any,
  theme: string,
  authentication: AuthenticationType,
  content: any,
  location: any,
  onLogout: ()=> void,
  setTheme: (name:string)=> void
}


/**
 * React component to manage Administration application.
 * This component display admin layout or login form if the user is not connected
 */
class AdminApp extends React.Component<AminAppProps, any> {
  constructor(){
    super();
    this.state = {
      instance: false
    }
    this.changeTheme = this.changeTheme.bind(this)
  }

  componentWillMount(){
    // Init admin theme
    let themeToSet = this.props.params.project;
    if (this.props.params.project === "instance"){
      this.setState({instance: true});
      themeToSet = "default";
    }
    this.props.setTheme(themeToSet)
  }

  changeTheme(themeToSet: string){
    if (this.props.theme !== themeToSet){
      this.props.setTheme(themeToSet)
    }
  }

  render(){
    const { theme, authentication, content, location, params, onLogout } = this.props
    const styles = getThemeStyles(theme, 'adminApp/styles')
    const commonStyles = getThemeStyles(theme,'common/common.scss')
    if (authentication){
      let authenticated = authentication.authenticateDate + authentication.user.expires_in > Date.now()
      authenticated = authenticated && (authentication.user.name !== undefined) && authentication.user.name !== 'public'
      if (authenticated === false){
        return (
          <div className={styles.main}>
            <Authentication />

            <SelectThemeComponent
              styles={commonStyles}
              themes={["cdpp","ssalto","default"]}
              curentTheme={theme}
              onThemeChange={this.changeTheme} />

              <SelectLocaleComponent
                styles={commonStyles}
                locales={['fr','en']} />
          </div>
        );
      } else {
          return (
            <div>
              <Layout
                location={location}
                content={content}
                project={params.project}
                onLogout={onLogout}/>

              <SelectThemeComponent
                styles={styles}
                themes={["cdpp","ssalto","default"]}
                curentTheme={theme}
                onThemeChange={this.changeTheme} />

                <SelectLocaleComponent
                  styles={commonStyles}
                  locales={['fr','en']} />
          </div>
          );
      }
    }
    else {
      return <ErrorComponent />
    }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state: any) => {
  return {
    theme: state.common.theme,
    authentication: state.common.authentication
  }
}
const mapDispatchToProps = (dispatch: any) => {
  return {
    setTheme: (theme: string) => {dispatch(setTheme(theme))},
    onLogout: () => {dispatch(logout())}
  }
}
const connectedAdminApp = connect<{}, {}, AminAppProps>(mapStateToProps,mapDispatchToProps)(AdminApp)
export default connectedAdminApp
