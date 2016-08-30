/** @module AdminApp */
import * as React from "react"
import SidebarContainer from "./modules/menu/containers/SidebarContainer"
import MenuContainer from "./modules/menu/containers/MenuContainer"
import IconMenu from "material-ui/IconMenu"
import MenuItem from "material-ui/MenuItem"
import IconButton from "material-ui/IconButton"
import MoreVertIcon from "material-ui/svg-icons/navigation/more-vert"
import { ThemeContextInterface, ThemeContextType } from "../common/theme/ThemeContainerInterface"

interface MainAdminLayoutProps {
  theme?: string,
  authentication?: any,
  content: any,
  location: any,
  onLogout?: () => void
}

const AdminAppBarIcon = (
  <div>
    <IconMenu
      iconButtonElement={<IconButton><MoreVertIcon /></IconButton>}
      anchorOrigin={{horizontal: 'left', vertical: 'top'}}
      targetOrigin={{horizontal: 'left', vertical: 'top'}}
    >
      <MenuItem primaryText="Refresh"/>
      <MenuItem primaryText="Send feedback"/>
      <MenuItem primaryText="Settings"/>
      <MenuItem primaryText="Help"/>
      <MenuItem primaryText="Sign out"/>
    </IconMenu>
  </div>
)

/**
 * React component to manage Administration application.
 * This component display admin layout or login form if the user is not connected
 */
class MainAdminLayout extends React.Component<MainAdminLayoutProps, any> {

  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  constructor () {
    super()
    this.state = {instance: false}
  }


  render (): JSX.Element {
    const {content} = this.props
    const style = {
      app: {
        classes: this.context.muiTheme.adminApp.layout.app.classes.join(' '),
        styles: this.context.muiTheme.adminApp.layout.app.styles,
      },
      bodyContainer: {
        classes: this.context.muiTheme.adminApp.layout.bodyContainer.classes.join(' '),
        styles: this.context.muiTheme.adminApp.layout.bodyContainer.styles,
      },
      contentContainer: {
        classes: this.context.muiTheme.adminApp.layout.contentContainer.classes.join(' '),
        styles: this.context.muiTheme.adminApp.layout.contentContainer.styles,
      },
    }

    return (
      <div className={style.app.classes} style={style.app.styles}>
        <MenuContainer/>
        <div className={style.bodyContainer.classes} style={style.bodyContainer.styles}>
          <SidebarContainer />
          <div className={style.contentContainer.classes} style={style.contentContainer.styles}>
            {content}
          </div>
        </div>
      </div>
    )
  }
}
export default MainAdminLayout
/*
 // Add theme from store to the component props
 const mapStateToProps = (state: any) => ({
 theme: state.common.theme,
 authentication: state.common.authentication
 })
 const mapDispatchToProps = (dispatch: any) => ({
 onLogout: () => {
 dispatch(logout())
 }
 })
 export default connect<{}, {}, MainAdminLayoutProps>(mapStateToProps, mapDispatchToProps)(MainAdminLayout)
 */
