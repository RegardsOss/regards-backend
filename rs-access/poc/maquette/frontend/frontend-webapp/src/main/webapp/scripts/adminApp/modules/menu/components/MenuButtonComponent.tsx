/** @module AdminLayout */
import * as React from 'react'
import { Link } from 'react-router'

interface MenuButtonProps {
    styles: any,
    label: string,
    to?: string,
    icon: string,
    onClick?: () => void
}

/**
 * React component to display an link button in the Menu.
 * Display a react-router Link if the prop to is set or a <a> link if the onClick prop is set
 * @prop {Object} styles CSS styles to apply
 * @prop {String} label Label of the link button
 * @prop {String} icon CSS class from fundation-icons to display the button icon
 * @prop {String} to [Optional] route of the link (route from react Router)
 * @prop {Function} onClick [Optional] callback on the button link click
 */
class MenuButtonComponent extends React.Component<MenuButtonProps, any> {
  render(){
    const { styles, label, to, icon, onClick } = this.props

    if(to)
      return (
        <Link
          to={to}
          className={styles.menuelement +" "+ styles.unselected}
          activeClassName={styles.selected}>
          <i className={icon} title={label}></i>
          <span>{label}</span>
        </Link>
      )
    else
      return (
        <a
          to={to}
          onClick={onClick}
          className={styles.menuelement +" "+ styles.unselected}>
          <i className={icon} title={label}></i>
          <span>{label}</span>
        </a>
      )
  }
}

export default MenuButtonComponent
