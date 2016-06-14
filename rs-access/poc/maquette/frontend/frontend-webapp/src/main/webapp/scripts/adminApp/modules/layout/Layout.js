import React from 'react'

import Menu from './components/MenuComponent'
import Home from '../home/Home'

class Layout extends React.Component {
  render(){
    // Add location to menu props in order to update view if location
    // change. This enable the activeClassName to update in the react
    // router links.
    const { styles, project, location, onLogout } = this.props;
    return (
      <div className={styles.layout}>
        <div className={styles.navigation}>
          <Menu
            styles={styles}
            onLogout={onLogout}
            project={project}
            location={location}/>
        </div>
        <div className={styles.content}>
          {this.props.content || <Home />}
        </div>
      </div>
    );
  }
}

Layout.propsTypes = {
  styles: React.PropTypes.object.isRequired,
  location: React.PropTypes.object.isRequired,
  content: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired,
  instance: React.PropTypes.bool.isRequired,
  onLogout: React.PropTypes.func.isRequired
}


export default Layout
