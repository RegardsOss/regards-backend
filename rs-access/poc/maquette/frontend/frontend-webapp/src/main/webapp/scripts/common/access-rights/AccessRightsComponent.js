import React from "react"
import { fetchAccessRights } from "./AccessRightsActions";

/**
* Root class for all RegardsView in each modules.
* This class handle the accessRights to the view modules.
*/
class AccessRightsComponent extends React.Component{

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
    this.checkViewAccessRights = this.checkViewAccessRights.bind(this);
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
  componentWillMount(){
    const { store } = this.context;
    if (this.getDependencies() === null){
      this.setState({
        access: true
      });
    } else {
      this.unsubscribe = store.subscribe(this.checkViewAccessRights);
      store.dispatch(fetchAccessRights(this.constructor.name, this.getDependencies()));
    }
  }

  componentWillUnmount(){
    if (this.unsubscribe){
      this.unsubscribe();
    }
  }

  checkViewAccessRights(){
    const { store } = this.context;
    const view = store.getState().views.find( curent => {
      return curent.name === this.constructor.name;
    });
    // If not, check access from server
    if (view){
      if (view.access === true){
        console.log("Access granted to view : " + this.constructor.name);
      } else {
        console.log("Access denied to view : " + this.constructor.name);
      }
      this.unsubscribe();
      this.setState({
        access: view.access
      });
    }
  }
}

AccessRightsComponent.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

export default AccessRightsComponent;
