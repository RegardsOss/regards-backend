import * as React from 'react'
import { connect } from 'react-redux'

import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'
import NavigationContainer from './containers/NavigationContainer'

interface LayoutProps {
  project: string,
  location: any
}

class Layout extends React.Component<LayoutProps, any> {

  render(){
    return (
      <div className="full-div">
        <div className="header">
          <h1> Test Application {this.props.project} </h1>
        </div>
        <AccessRightsComponent dependencies={null}>
          <NavigationContainer project={this.props.project} location={this.props.location}/>
        </AccessRightsComponent>
        <div>
          {this.props.children}
        </div>
      </div>
    )
  }
}

export default Layout
