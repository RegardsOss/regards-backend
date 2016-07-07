import React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from 'common/theme/ThemeUtils'
import { setTheme } from 'common/theme/actions/ThemeActions'
import SelectThemeComponent from 'common/theme/components/SelectThemeComponent'

import NavigationContainer from './containers/NavigationContainer'

class Layout extends React.Component {

  render(){
    const { theme } = this.props
    const styles = getThemeStyles(this.props.theme, 'userApp/base')
    const commonStyles = getThemeStyles(theme,'common/common.scss')
    return (
      <div className="full-div">
        <div className="header">
          <h1> Test Application {this.props.project} </h1>
        </div>
        <NavigationContainer project={this.props.project} location={this.props.location}/>
        <div className={styles.main}>
          {this.props.children}
        </div>
        <SelectThemeComponent
          styles={commonStyles}
          themes={["cdpp","ssalto","default"]}
          curentTheme={theme}
          onThemeChange={this.props.setTheme} />
      </div>
    )
  }
}

Layout.propTypes = {
  project: React.PropTypes.string.isRequired
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.common.theme,
    plugins: state.common.plugins
  }
}
const mapDispatchToProps = (dispatch) => {
  return {
    setTheme: (theme) =>  dispatch(setTheme(theme))
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(Layout)
