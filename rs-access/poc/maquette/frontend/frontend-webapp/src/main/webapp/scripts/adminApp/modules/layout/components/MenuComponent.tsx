import * as React from 'react'
import { connect } from 'react-redux'
import { IndexLink, Link } from 'react-router'
import MenuButtonComponent from './MenuButtonComponent'

// Styles
var classnames = require("classnames")
//import '../../../../stylesheets/foundation-icons/foundation-icons.scss'
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'

interface MenuProps {
  theme: string,
  project: string,
  onLogout?: () => void,
  location: any
}

class Menu extends React.Component<MenuProps, any> {

  render(){
    const { theme, project } = this.props
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
              icon="fi-power">
            </MenuButtonComponent>
          </li>
          <li>
            <MenuButtonComponent
              to={"/admin/"+project+"/projects"}
              styles={styles}
              label="Projects"
              icon="fi-widget">
            </MenuButtonComponent>
          </li>
        </ul>
      </div>
    )
  }
}

export default Menu
