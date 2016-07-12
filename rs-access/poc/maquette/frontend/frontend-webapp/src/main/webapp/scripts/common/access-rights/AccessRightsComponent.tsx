import * as React from "react"
import { fetchAccessRights } from "./AccessRightsActions"

import { AccessRightsView, Dependencies } from "./AccessRightsViewType"


interface AccessRightsTypes {
  dependencies:Dependencies
  fetchAccessRights?: (dependencies:Dependencies)=> void
}

/**
* Root class for all RegardsView in each modules.
* This class handle the accessRights to the view modules.
*/
class AccessRightsComponent<P, S> extends React.Component<AccessRightsTypes, any>{

  unsubscribeViewAccessRights:any = null
  oldRender:any = null

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
    return this.props.dependencies;
  }

  render() {
    return (
      <div>
        {this.props.children}
      </div>
    )
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
      this.oldRender = Object.assign({}, this.render)
      this.render = () => {return null}
      store.dispatch(fetchAccessRights(this.getDependencies()))
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
        // Activate component render
        this.render = this.oldRender;
      } else {
        console.log("Access denied to view : " + this.constructor.name)
      }
      this.unsubscribeViewAccessRights()
      this.setState({
        access: view.access
      });
    }
  }
}

export default AccessRightsComponent
