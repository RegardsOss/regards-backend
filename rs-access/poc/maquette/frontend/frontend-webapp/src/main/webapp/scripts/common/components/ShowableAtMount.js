import * as React from "react"

class ShowableAtMount extends React.Component{

  constructor() {
    super()
    this.oldRender = this.render
    this.render = () => {return null}
  }

  render() {
    return (<div>{this.props.children}</div>)
  }

  componentWillMount(){
    if(this.props.show)
      this.render = this.oldRender
  }

}


ShowableAtMount.PropTypes = {
  show: React.PropTypes.bool
}

export default ShowableAtMount
