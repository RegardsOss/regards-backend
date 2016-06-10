
import React from 'react';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';

import { setTheme } from 'common/theme/actions/ThemeActions';
import { getThemeStyles } from 'common/theme/ThemeUtils';
import Authentication from './modules/authentication/Authentication';
import SelectThemeComponent from 'common/theme/components/SelectThemeComponent';
import Layout from './modules/layout/Layout';

class AdminApp extends React.Component {
  constructor(){
    super();
    this.state = {
      instance: false
    }
  }

  componentWillMount(){
    // Init admin theme
    let themeToSet = this.props.params.project;
    if (this.props.params.project === "instance"){
      this.setState({instance: true});
      themeToSet = "default";
    }
    this.props.setTheme(themeToSet);
  }


  changeTheme(themeToSet){
    if (this.props.theme !== themeToSet){
      console.log("changing theme to : " + themeToSet );
      this.props.setTheme(themeToSet);
    }
  }

  render(){
    const { theme, authentication, content, location, params } = this.props;
    const styles = getThemeStyles(theme, 'adminApp/styles');

    const authenticated = authentication.authenticateDate + authentication.user.expires_in > Date.now();
    if (!authenticated || authentication.user.name === 'public'){
      return (
        <div className={styles.main}>
          <Authentication
            project={params.project}
            onAuthenticate={this.onAuthenticate}/>
          <SelectThemeComponent styles={styles} themes={["cdpp","ssalto","default"]} curentTheme={theme} onThemeChange={(theme) => this.changeTheme(theme)} />
        </div>
      );
    } else {
        return (
          <Layout
            location={location}
            content={content}
            project={params.project}
            instance={this.state.instance}/>
        );
    }
  }
}

AdminApp.contextTypes = {
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    authentication: state.authentication
  }
}
const mapDispatchToProps = (dispatch) => {
  return {
    setTheme: (theme) => {dispatch(setTheme(theme))}
  }
}
module.exports = connect(mapStateToProps,mapDispatchToProps)(AdminApp);
