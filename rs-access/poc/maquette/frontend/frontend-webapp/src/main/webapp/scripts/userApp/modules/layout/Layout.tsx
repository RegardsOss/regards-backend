import * as React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from '../../../common/theme/ThemeUtils'
import { setTheme } from '../../../common/theme/actions/ThemeActions'
import SelectTheme from '../../../common/theme/containers/SelectTheme'
import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'
import NavigationContainer from './containers/NavigationContainer'

interface LayoutProps {
  project: string,
  location: any,
  // Properties set by react redux connection
  theme?: string,
  setTheme?: (theme:string) => void
}

class Layout extends React.Component<LayoutProps, any> {

  render(){
    const { theme } = this.props
    const styles = getThemeStyles(this.props.theme, 'userApp/base')
    const commonStyles = getThemeStyles(theme,'common/common.scss')

    return (
      <div className="full-div">
        <div className="header">
          <h1> Test Application {this.props.project} </h1>
        </div>
        <AccessRightsComponent dependencies={null}>
          <NavigationContainer project={this.props.project} location={this.props.location}/>
        </AccessRightsComponent>
        <div className={styles.main}>
          {this.props.children}
        </div>
      </div>
    )
  }
}

// Add theme from store to the component props
const mapStateToProps = (state:any) => {
  return {
    theme: state.common.theme
  }
}
const mapDispatchToProps = (dispatch:any) => {
  return {
    setTheme: (theme:string) =>  dispatch(setTheme(theme))
  }
}
export default connect<{}, {}, LayoutProps>(mapStateToProps,mapDispatchToProps)(Layout)
