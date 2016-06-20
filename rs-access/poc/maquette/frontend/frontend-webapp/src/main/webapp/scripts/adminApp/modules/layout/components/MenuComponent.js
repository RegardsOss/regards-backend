import React from 'react'
import { connect } from 'react-redux'
import { IndexLink, Link } from 'react-router'
import MenuButtonComponent from './MenuButtonComponent'

import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

class Menu extends React.Component {

  render(){
    const { onLogout, project, styles } = this.props


    return (
      <div className={styles["icon-bar"] + ' ' + styles["four-up"] + ' ' + styles["medium-vertical"]}>

        <MenuButtonComponent
          styles={styles}
          label=""
          icon={icons["fi-power"]}>
        </MenuButtonComponent>

        <MenuButtonComponent
          to={"/admin/"+project+"/projects"}
          styles={styles}
          label="Projects"
          icon={icons["fi-power"]}>
        </MenuButtonComponent>

        <MenuButtonComponent
          to={"/admin/"+project+"/test"}
          styles={styles}
          label="Test"
          icon={icons["fi-power"]}>
        </MenuButtonComponent>

      </div>
      )
  }
}

Menu.propTypes = {
  styles: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired,
  onLogout: React.PropTypes.func.isRequired,
}

export default Menu
