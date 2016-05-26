import React from "react"

/**
* Root class for all RegardsView in each modules.
* This class handle the accessRights to the view modules.
*/
class RegardsView extends React.Component{

  constructor(){
    super();
    this.state= {
      access: false
    }
  }

  /**
  * Method to check if the view is displayable
  */
  checkViewAccessRights(){
    const { store } = this.context;
    let access = false;
    for (var i=0; i < store.getState().views.length;i++) {
        if (access === false && store.getState().views[i].name === this.constructor.name){
          console.log("Access granted to view " + this.constructor.name);
          access = true;
        }
    }
    this.setState({
      access: access
    });
  }

  componentWillMount(){
    const { store } = this.context;
    this.checkViewAccessRights();
    this.unsubscribe = store.subscribe(() => { this.checkViewAccessRights()});
  }

  componentWillUnmount(){
    this.unsubscribe();
  }

  renderView(){
    throw "renderView method is not implemented for the given view : " + this.constructor.name;
  }

  render(){
    if (this.state.access === true){
      return this.renderView();
    } else {
      console.log("Access denied to view : "+ this.constructor.name);
      return null;
    }
  }

}

RegardsView.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

export default RegardsView;
