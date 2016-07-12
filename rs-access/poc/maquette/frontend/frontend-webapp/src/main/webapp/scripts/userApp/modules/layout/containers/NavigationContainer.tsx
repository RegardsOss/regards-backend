import * as React from 'react'
import { connect } from 'react-redux'

import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import { Dependencies } from '../../../../common/access-rights/AccessRightsViewType'
import { PluginsStore } from '../../../../common/plugins/PluginTypes'
import LinkComponent from '../components/LinkComponent'

interface NavigationProps {
  project: string,
  location: any,
  // Properties set by react redux connection
  plugins?: PluginsStore
}

class NavigationContainer extends AccessRightsComponent<NavigationProps, any> {

  getDependencies():Dependencies{
    return null
  }

  render(){
    const { location, plugins, project } = this.props
    if (this.state.access === true && plugins.items){
      return (
        <nav>
          <LinkComponent location={location} key="plop" to={"/user/"+project+"/test"}>Test de lien</LinkComponent>
          <LinkComponent location={location} key="time" to={"/user/"+project+"/time"}>Temps</LinkComponent>
          {plugins.items.map( plugin => {
            if (plugin && plugin.plugin){
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
    }
    return null
  }
}

// Add projects from store to the container props
const mapStateToProps = (state: any) => {
  return {
    plugins: state.plugins
  }
}
const navigation = connect<{}, {}, NavigationProps>(mapStateToProps)(NavigationContainer);
export default navigation
