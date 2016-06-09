import React from 'react';
import { connect } from 'react-redux';
import { getThemeStyles } from 'common/theme/ThemeUtils';

class Test extends React.Component {
  render(){
    const styles = getThemeStyles(this.props.theme, 'adminApp/styles');
    return (
      <div className={styles["grid-basics-example"]}>
        <div className={styles.row + " " + styles.display}>
          <div className={styles.columns + " " + styles["small-2"] + " " + styles["large-4"]}>4 columns</div>
          <div className={styles.columns + " " + styles["small-4"] + " " + styles["large-4"]}>4 columns</div>
          <div className={styles.columns + " " + styles["small-6"] + " " + styles["large-4"]}>4 columns</div>
        </div>
        <div className={styles.row + " " + styles.display}>
          <div className={styles["large-3"]}>3 columns</div>
          <div className={styles["large-6"]}>6 columns</div>
          <div className={styles["large-3"]}>3 columns</div>
        </div>
        <div className={styles.row + " " + styles.display}>
          <div className={styles["small-6"] + " " + styles["large-2"]}>2 columns</div>
          <div className={styles["small-6"] + " " + styles["large-8"]}>8 columns</div>
          <div className={styles["small-12"] + " " + styles["large-2"]}>2 columns</div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
module.exports = connect(mapStateToProps)(Test);
