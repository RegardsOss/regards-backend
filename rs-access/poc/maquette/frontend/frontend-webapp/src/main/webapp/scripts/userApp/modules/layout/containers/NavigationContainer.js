import React from 'react'
import { connect } from 'react-redux'

import LinkComponent from '../components/LinkComponent'

class NavigationContainer extends React.Component {

  render(){
    const { location, plugins, project } = this.props
    if (plugins.items){
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

NavigationContainer.propTypes = {
  project: React.PropTypes.string.isRequired,
  location: React.PropTypes.object.isRequired,
}

// Add projects from store to the container props
const mapStateToProps = (state) => {
  return {
    plugins: state.common.plugins
  }
}
module.exports = connect(mapStateToProps)(NavigationContainer)
