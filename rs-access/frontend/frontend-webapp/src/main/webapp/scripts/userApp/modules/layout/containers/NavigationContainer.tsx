import * as React from "react";
import { connect } from "react-redux";
import { PluginsStore } from "../../../../common/plugins/PluginTypes";
import { fetchPlugins } from "../../../../common/plugins/PluginsActions";
import LinkComponent from "../components/LinkComponent";


interface NavigationProps {
  project: string,
  location: any,
  // Properties set by react redux connection
  plugins?: PluginsStore,
  fetchPlugins?: () => void
}

class NavigationContainer extends React.Component<NavigationProps, any> {

  componentWillMount() {
    // Plugins are set to the container props by react-redux connect.
    // See method mapStateToProps of this container
    const {plugins} = this.props
    // initTheme method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    // this.props.initTheme(themeToSet)

    if (!plugins || !plugins.items || plugins.items.length === 0) {
      // fetchPlugins method is set to the container props by react-redux connect.
      // See method mapDispatchToProps of this container
      this.props.fetchPlugins ()
    }
  }

  render() {
    const {location, plugins, project} = this.props
    if (plugins.items) {
      return (
        <nav>
          <LinkComponent location={location} key="test" to={"/user/"+project+"/test"}>Test de lien</LinkComponent>
          <LinkComponent location={location} key="time" to={"/user/"+project+"/time"}>Temps</LinkComponent>
          {plugins.items.map (plugin => {
            if (plugin) {
              return (
                <LinkComponent
                  location={location}
                  key={plugin.name}
                  to={"/user/" + project + "/plugins/" + plugin.name}>
                  {plugin.name}
                </LinkComponent>
              )
            }
          })}
        </nav>
      )
    } else {
      return (
        <nav>
          <LinkComponent location={location} key="test" to={"/user/"+project+"/test"}>Test de lien</LinkComponent>
          <LinkComponent location={location} key="time" to={"/user/"+project+"/time"}>Temps</LinkComponent>
        </nav>
      )
    }
  }
}

// Add projects from store to the container props
const mapStateToProps = (state: any) => {
  return {
    plugins: state.common.plugins
  }
}
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch: any) => ({
  fetchPlugins: () => dispatch (fetchPlugins ())
})
const navigation = connect<{}, {}, NavigationProps> (mapStateToProps, mapDispatchToProps) (NavigationContainer);
export default navigation
