import React from 'react'
import { Link } from 'react-router'
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

// Performs a <Link> encapsulation
class MenuButtonComponent extends React.Component {
  render(){
    const { styles, label, to, icon } = this.props

    if(to)
      return (
        <Link
          to={to}
          className={styles.menuelement +" "+ styles.unselected}
          activeClassName={styles.selected}>
          <i className={icon} title={label}></i>
          {label}
        </Link>
      )
    else
      return (
        <span
          to={to}
          className={styles.menuelement +" "+ styles.unselected}>
          <i className={icon} title={label}></i>
          {label}
        </span>
      )
  }
}

MenuButtonComponent.propTypes = {
  styles: React.PropTypes.object.isRequired,
  label: React.PropTypes.string,
  to: React.PropTypes.string,
  icon: React.PropTypes.string
}

export default MenuButtonComponent
