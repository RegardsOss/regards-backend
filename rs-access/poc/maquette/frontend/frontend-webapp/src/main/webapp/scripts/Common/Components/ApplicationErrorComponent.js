import React from 'react';

import styles from 'common/common';

class ApplicationErrorComponent extends React.Component {

  render(){
    return (
      <div className={styles.errorApp}>
        Application unavailable
      </div>
    );
  }
}

export default ApplicationErrorComponent;
