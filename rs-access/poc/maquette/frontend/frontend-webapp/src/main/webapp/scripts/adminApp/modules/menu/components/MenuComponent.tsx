/** @module AdminLayout */
import * as React from 'react'
import { connect } from 'react-redux'
import { IndexLink, Link } from 'react-router'
import MenuButtonComponent from './MenuButtonComponent'
// Styles
var classnames = require("classnames")
var icons = require('../../../../../stylesheets/foundation-icons/foundation-icons.scss')
// import { getThemeStyles } from '../../../../common/theme/ThemeUtils'

interface MenuProps {
  theme: string,
  project: string,
  onLogout?: () => void,
  location: any
}


/**
 * React Menu component. Display the admin application menu
 * @prop {String} theme CSS Theme
 * @prop {String} project Project
 * @prop {Object} location react-router location
 */
class Menu extends React.Component<MenuProps, any> {

  render(){
    const { theme, project } = this.props
    // const styles = getThemeStyles(theme, 'adminApp/styles')
    // const menuClassName = classnames(
      // styles['menuContainer']
      // styles['columns'],
      // styles['large-1'],
      // styles['small-12']
    // )
    // const ulClassName = classnames(styles['menu'], styles['vertical'], styles['icon-top'])

    return (
      <div>
        <ul>
          <li>
            <MenuButtonComponent
              styles={null}
              onClick={this.props.onLogout}
              label="Logout"
              icon={icons["fi-power"]}>
            </MenuButtonComponent>
          </li>
          <li>
            <MenuButtonComponent
              to={"/admin/"+project+"/projects"}
              styles={null}
              label="Projects"
              icon={icons["fi-widget"]}>
            </MenuButtonComponent>
          </li>
        </ul>
      </div>
    )
  }
}

export default Menu
