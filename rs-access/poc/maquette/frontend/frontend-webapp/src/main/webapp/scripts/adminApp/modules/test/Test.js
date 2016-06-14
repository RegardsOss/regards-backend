import React from 'react'
import { connect } from 'react-redux'
import { getThemeStyles } from 'common/theme/ThemeUtils'
import { browserHistory } from 'react-router'

class Test extends React.Component {

  constructor(){
    super();
    this.state = {
      inputValue:''
    }
    this.onChange = this.onChange.bind(this)
    this.onClick = this.onClick.bind(this)
  }

  componentWillMount(){
    // Read query parameters
    const { value } = this.props.location.query
    this.setState({
      inputValue: value
    })
  }

  onChange(e){
    let { value } = this.props.location.query
    value = e.target.value
    this.setState({
      inputValue: e.target.value
    })
  }

  onClick(){
    // Update query parameters of the current url
    const { location } = this.props
    const route = location.pathname + "?value=" + this.state.inputValue
    browserHistory.push(route)
  }
  render(){
    const styles = getThemeStyles(this.props.theme, 'adminApp/styles');
    return (
      <div className={styles["grid-basics-example"]}>
        <input type='text' value={this.state.inputValue} onChange={this.onChange}/>
        <input type="button" onClick={this.onClick} value="update"/>
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

Test.contextTypes = {
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
module.exports = connect(mapStateToProps)(Test)
