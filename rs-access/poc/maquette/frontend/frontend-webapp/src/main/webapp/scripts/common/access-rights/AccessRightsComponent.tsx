import * as React from "react"
import { fetchAccessRights } from "./AccessRightsActions"


interface AccessRightsTypes {
  store: any,
  router: any,
  route : any
}

/**
* Root class for all RegardsView in each modules.
* This class handle the accessRights to the view modules.
*/
class AccessRightsComponent extends React.Component<AccessRightsTypes, any>{

  unsubscribeViewAccessRights:any = null

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
    this.checkViewAccessRights = this.checkViewAccessRights.bind(this)
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
  getDependencies():Dependencies{
    return null;
  }

  /**
  * Method to check if the view is displayable
  */
  componentWillMount(){
    const { store }:any = this.context
    if (this.getDependencies() === null){
      this.setState({
        access: true
      });
    } else {
      this.unsubscribeViewAccessRights = store.subscribe(this.checkViewAccessRights)
      store.dispatch(fetchAccessRights(this.constructor.name, this.getDependencies()))
    }
  }

  componentWillUnmount(){
    if (this.unsubscribeViewAccessRights){
      this.unsubscribeViewAccessRights()
    }
  }

  checkViewAccessRights(){
    const { store }:any = this.context
    const view = store.getState().views.find( (curent:AccessRightsView) => {
      return curent.name === this.constructor.name
    });
    // If not, check access from server
    if (view){
      if (view.access === true){
        console.log("Access granted to view : " + this.constructor.name)
      } else {
        console.log("Access denied to view : " + this.constructor.name)
        // Cancel render method
        this.render = () => {return null}
      }
      this.unsubscribeViewAccessRights()
      this.setState({
        access: view.access
      });
    }
  }
}

export default AccessRightsComponent
