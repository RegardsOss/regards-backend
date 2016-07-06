import React from 'react'
import { connect } from 'react-redux'
import { IndexLink, Link } from 'react-router'
import MenuButtonComponent from './MenuButtonComponent'
// Styles
import classnames from 'classnames'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
import { getThemeStyles } from 'common/theme/ThemeUtils'

class Menu extends React.Component {

  render(){
    const { theme, onLogout, project } = this.props
    const styles = getThemeStyles(theme, 'adminApp/styles')
    const menuClassName = classnames(
      styles['menuContainer']
      // styles['columns'],
      // styles['large-1'],
      // styles['small-12']
    )
    const ulClassName = classnames(styles['menu'], styles['vertical'], styles['icon-top'])

    return (
      <div className={menuClassName}>
        <ul className={ulClassName}>
          <li>
            <MenuButtonComponent
              to={"/admin/"+project+"/test"}
              styles={styles}
              label="Logout"
              icon={icons["fi-power"]}>
            </MenuButtonComponent>
          </li>
          <li>
            <MenuButtonComponent
              to={"/admin/"+project+"/projects"}
              styles={styles}
              label="Projects"
              icon={icons["fi-widget"]}>
            </MenuButtonComponent>
          </li>
        </ul>
      </div>
    )
  }
}

Menu.propTypes = {
  theme: React.PropTypes.string.isRequired,
  project: React.PropTypes.string.isRequired,
  onLogout: React.PropTypes.func.isRequired,
}

export default Menu

// <li><a href="#"><i className={liClassName}></i> <span>One</span></a></li>
// <li><a href="#"><i className={liClassName}></i> <span>Two</span></a></li>
// <li><a href="#"><i className={liClassName}></i> <span>Three</span></a></li>
// <li><a href="#"><i className={liClassName}></i> <span>Four</span></a></li>
