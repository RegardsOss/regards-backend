import React from 'react'
import { connect } from 'react-redux'

import { fetchPlugins } from 'common/plugins/PluginsActions'
import { setTheme } from 'common/theme/actions/ThemeActions'
import Layout from './modules/layout/Layout'
import Test from './modules/test/Test'

class UserApp extends React.Component {

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
const mapDispatchToProps = (dispatch) => {
  return {
    fetchPlugins : () => dispatch(fetchPlugins()),
    initTheme : (theme) => dispatch(setTheme(theme))
  }
}
const mapStateToProps = (state) => {
  return {
    plugins: state.plugins
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(UserApp)
