import * as React from "react"

class ShowableAtRender extends React.Component{

  render() {
    if(this.props.show)
      return <div>{this.props.children}</div>
    else
      return null
  }

}

ShowableAtRender.PropTypes = {
  show: React.PropTypes.bool
}

export default ShowableAtRender
