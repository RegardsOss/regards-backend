import React from "react"
import { Rest } from "grommet";
import { addViewAccess } from "./RegardsModuleActionCreators";

/**
* Root class for all RegardsView in each modules.
* This class handle the accessRights to the view modules.
*/
class RegardsView extends React.Component{

  /**
  * Constructor.
  * By default the access rights to the view is false.
  * Define dependencies for access rights management.
  */
  constructor(){
    super();
    this.state= {
      access: false
    }
  }

  /**
  * Method to get the REST dependencies of the view.
  * If all the dependencies are authorized, then the view can be displayed.
  * return null for no dependencies or an object :
  * {
  *  GET : ["dependence"],
  *  POST : ["dependence"],
  *  PUT : ["dependence"],
  *  DELETE : ["dependence"],
  * }
  */
  getDependencies(){
    return null;
  }

  /**
  * Method to check if the view is displayable
  */
  checkViewAccessRights(){
    const { store } = this.context;
    let found = false;
    let access = false;
    // Check if view access is already in store
    for (var i=0; i < store.getState().views.length;i++) {
        if (store.getState().views[i].name === this.constructor.name){
          access = store.getState().views[i].access;
          found=true;
        }
    }

    // If not, check access from server
    if (!found){
      const location = window.location.origin + "/rest/access/rights";
      if (this.getDependencies() !== null){
        Rest.get(location,this.getDependencies()).end((error, response) => {
          if (response.status === 200){
            console.log("Access granted to view " + this.constructor.name);
            store.dispatch(addViewAccess(this.constructor.name,true));
          } else {
            store.dispatch(addViewAccess(this.constructor.name,false));
            console.log("Access denied to view : "+ this.constructor.name);
          }
        });
      } else {
        console.log("Access granted to view " + this.constructor.name);
        store.dispatch(addViewAccess(this.constructor.name,true));
      }
    } else{
      this.setState({
        access: access
      });
    }
  }

  componentWillMount(){
    const { store } = this.context;
    this.unsubscribe = store.subscribe(() => { this.checkViewAccessRights()});
    this.checkViewAccessRights();
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
