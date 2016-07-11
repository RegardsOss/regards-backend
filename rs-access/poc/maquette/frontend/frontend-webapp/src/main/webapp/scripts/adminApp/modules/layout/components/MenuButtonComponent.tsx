import * as React from 'react'
import { Link } from 'react-router'

interface MenuButtonProps {
    styles: any,
    label: string,
    to?: string,
    icon: string,
    onClick?: () => void
}

// Performs a <Link> encapsulation
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
        <span
          to={to}
          onClick={onClick}
          className={styles.menuelement +" "+ styles.unselected}>
          <i className={icon} title={label}></i>
          <span>{label}</span>
        </span>
      )
  }
}

export default MenuButtonComponent
