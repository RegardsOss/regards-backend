import React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from 'common/theme/ThemeUtils'

class ApplicationErrorComponent extends React.Component {

  render(){
    const styles = getThemeStyles(this.props.theme, 'common/common')
    return (
      <div className={styles.errorApp}>
        Application unavailable
      </div>
    );
  }
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(ApplicationErrorComponent)
