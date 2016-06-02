import React from 'react';

import { getThemeStyles } from 'Common/ThemeUtils';

class ApplicationErrorComponent extends React.Component {

  render(){
    const styles = getThemeStyles(this.props.theme, 'common/common');
    return (
      <div className={styles.errorApp}>
        Application unavailable
      </div>
    );
  }
}

export default ApplicationErrorComponent;
