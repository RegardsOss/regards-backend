/** @module PortalApp */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import * as ReactDOM from 'react-dom'

import ApplicationErrorComponent from '../common/components/ApplicationErrorComponent'
import SelectTheme from '../common/theme/containers/SelectTheme'
import InstanceComponent from './modules/projects/components/InstanceComponent'
import ProjectsContainer from './modules/projects/containers/ProjectsContainer'
import { getThemeStyles } from '../common/theme/ThemeUtils'

import { fetchAuthenticate } from '../common/authentication/AuthenticateActions'

interface PortalAppProps {
  // Properties set by react-redux connectiona
  authentication?: any,
  theme?: string,
  publicAuthenticate?: ()=> void,
}


/**
 * React component to manage portal application.
 */
class PortalApp extends React.Component<PortalAppProps, any> {

  componentWillMount(){
    // Init application theme
    // initTheme and publicAuthenticate methods are set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.publicAuthenticate()
  }

  render(){
    // authentication and theme are set in this container props by react-redux coonect.
    // See method mapStateToProps
    const { authentication, theme } = this.props
    // Get theme styles
    const styles = getThemeStyles(theme,'portalApp/styles')
    const commonStyles = getThemeStyles(theme,'common/common.scss')

    // if (!authentication || authentication.isFetching === true || !authentication.user || !authentication.user.access_token){
    if (!authentication || !authentication.user){
      // If no user connected, display the error component
      return <ApplicationErrorComponent />
    } else if (this.props.children){
      // If a children of this container is defined display it.
      // The children is set by react-router if any child route is reached.
      // The possible children of portal are define in the main routes.js.
      return (<div>{this.props.children}</div>)
    } else {
      // Else, display the portal
      return (
        <div className={styles.main}>
          <InstanceComponent styles={styles}/>
          <ProjectsContainer styles={styles}/>
          <SelectTheme />
        </div>
      )
    }
  }
}

// Add props from store to the container props
const mapStateToProps = (state:any) => ({
  authentication: state.common.authentication
})
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch:any) => ({
  publicAuthenticate: () => dispatch(fetchAuthenticate("public","public")),
})
export default connect<{}, {}, PortalAppProps>(mapStateToProps,mapDispatchToProps)(PortalApp)
