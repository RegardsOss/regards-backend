import * as React from 'react'
import { connect } from 'react-redux'

import { fetchPlugins } from '../common/plugins/PluginsActions'
import { setTheme } from '../common/theme/actions/ThemeActions'
import Layout from './modules/layout/Layout'
import Test from './modules/test/Test'

import { PluginsStore } from '../common/plugins/PluginTypes'

interface UserAppProps {
  plugins: PluginsStore,
  params:any,
  initTheme: (theme:string) => void,
  fetchPlugins: () => void,
  location: any,
  content:any
}

class UserApp extends React.Component<UserAppProps, any> {

  componentWillMount(){
    // Get project from params from react router. project param is the ":project" in userApp route
    // See routes.js
    const themeToSet = this.props.params.project
    // Plugins are set to the container props by react-redux connect.
    // See method mapStateToProps of this container
    const { plugins } = this.props
    // initTheme method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.initTheme(themeToSet)

    if (!plugins || !plugins.items || plugins.items.length === 0){
      // fetchPlugins method is set to the container props by react-redux connect.
      // See method mapDispatchToProps of this container
      this.props.fetchPlugins()
    }
  }

  render(){
    // Location ,params and content are set in this container props by react-router
    const { location, params, content } = this.props
    const { project } = params
    if (!content){
      return (<Layout location={location} project={project}><Test /></Layout>)
    } else {
      return (<Layout location={location} project={project}>{this.props.content}</Layout>)
    }
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch: any) => {
  return {
    fetchPlugins : () => dispatch(fetchPlugins()),
    initTheme : (theme:string) => dispatch(setTheme(theme))
  }
}
const mapStateToProps = (state:any) => {
  return {
    plugins: state.plugins
  }
}
const userAppConnected = connect<{}, {}, UserAppProps>(mapStateToProps,mapDispatchToProps)(UserApp)
export default userAppConnected
