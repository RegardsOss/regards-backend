import * as React from "react"
import { connect } from 'react-redux'

import { fetchAccessRights } from "./AccessRightsActions"
import { AccessRightsView, Dependencies } from "./AccessRightsViewType"


interface AccessRightsTypes {
  dependencies:Dependencies,
  // Properties set by react redux connection
  views?:Array<AccessRightsView>,
  doFetchAccessRights?: (dependencies:Dependencies)=> void
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
    if (this.getDependencies() === null){
      this.setState({
        access: true
      });
    } else {
      this.oldRender = Object.assign({}, this.render)
      this.render = () => {return null}
      this.props.doFetchAccessRights(this.getDependencies())
    }
  }

  checkViewAccessRights(){
    const view = this.props.views.find( (curent:AccessRightsView) => {
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
    }
  }
}

const mapStateToProps = (state:any) => ({
  views: state.views
})
const mapDispatchToProps = (dispatch:any) => ({
  doFetchAccessRights: (dependencies:Dependencies) => dispatch(fetchAccessRights(dependencies))
})

export default connect<{}, {}, AccessRightsTypes>(mapStateToProps, mapDispatchToProps)(AccessRightsComponent)
