/** @module common */
import * as React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from '../theme/ThemeUtils'


/**
 * React component to display a global application error.
 */
class ApplicationErrorComponent extends React.Component<any, any> {

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
const mapStateToProps = (state: any) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(ApplicationErrorComponent)
