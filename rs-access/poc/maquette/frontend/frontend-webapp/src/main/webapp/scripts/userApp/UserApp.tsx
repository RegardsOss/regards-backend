import * as React from 'react'
import { connect } from 'react-redux'

import { fetchPlugins } from '../common/plugins/PluginsActions'
import Layout from './modules/layout/Layout'
import TestContainer from './modules/test/TestContainer'

import { PluginsStore } from '../common/plugins/PluginTypes'

// Theme
import ThemeHelper from '../common/theme/ThemeHelper'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import SelectTheme from '../common/theme/containers/SelectTheme'

interface UserAppProps {
  plugins: PluginsStore,
  params:any,
  fetchPlugins: () => void,
  location: any,
  content:any,
  theme: string
}

class UserApp extends React.Component<UserAppProps, any> {

  componentWillMount(){
    // Plugins are set to the container props by react-redux connect.
    // See method mapStateToProps of this container
    const { plugins } = this.props
    // initTheme method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    // this.props.initTheme(themeToSet)

    if (!plugins || !plugins.items || plugins.items.length === 0){
      // fetchPlugins method is set to the container props by react-redux connect.
      // See method mapDispatchToProps of this container
      this.props.fetchPlugins()
    }
  }

  render(){
    // Location ,params and content are set in this container props by react-router
    const { location, params, content, theme } = this.props
    const { project } = params

    // Build theme
    const muiTheme = ThemeHelper.getByName(theme)

    if (content){
      return (
        <MuiThemeProvider muiTheme={muiTheme}>
          <Layout location={location} project={project} >
            <TestContainer />
          </Layout>
        </MuiThemeProvider>
       )
    } else {
      return (
        <MuiThemeProvider muiTheme={muiTheme}>
          <div>
            <SelectTheme />
            <Layout location={location} project={project}>
              {this.props.content}
            </Layout>
          </div>
        </MuiThemeProvider>
      )
    }
  }
}

const mapStateToProps = (state:any) => ({
  theme: state.common.theme,
  plugins: state.plugins
})
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch: any) => ({
  fetchPlugins : () => dispatch(fetchPlugins())
})
const userAppConnected = connect<{}, {}, UserAppProps>(mapStateToProps,mapDispatchToProps)(UserApp)
export default userAppConnected
