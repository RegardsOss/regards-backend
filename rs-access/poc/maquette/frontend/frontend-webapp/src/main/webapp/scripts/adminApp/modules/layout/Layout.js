import React from 'react'
import { connect } from 'react-redux'
import MenuComponent from './components/MenuComponent'
import Home from '../home/Home'
// Styles
import { getThemeStyles } from 'common/theme/ThemeUtils'
import classnames from 'classnames'

class Layout extends React.Component {
  render(){
    // Add location to menu props in order to update view if location
    // change. This enable the activeClassName to update in the react
    // router links.
    const { theme, project, location, onLogout } = this.props;
    const styles = getThemeStyles(theme, 'adminApp/styles')
    const layoutClassName = classnames(styles['layout'], styles['row'])
    const contentClassName = classnames(styles['content'], styles['small-12'], styles['large-11'], styles['columns'])

    return (
      <div className={layoutClassName}>
        <MenuComponent
          theme={theme}
          onLogout={onLogout}
          project={project}
          location={location}/>
        <div className={contentClassName}>
          {this.props.content || <Home />}
        </div>
      </div>
    );
  }
}

Layout.propsTypes = {
  theme: React.PropTypes.object.isRequired,
  location: React.PropTypes.object.isRequired,
  content: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired,
  instance: React.PropTypes.bool.isRequired,
  onLogout: React.PropTypes.func.isRequired
}

// Add theme from store to the component props
const mapStateToProps = (state) => ({
  theme: state.common.theme
})
export default connect(mapStateToProps)(Layout)
