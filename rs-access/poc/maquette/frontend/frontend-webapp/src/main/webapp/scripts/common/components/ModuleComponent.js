import * as React from 'react'
import ShowableAtMount from 'common/components/ShowableAtMount'

class ModuleComponent extends React.Component {
  getModuleVisibility() {
    return false
  }

  render() {
    return (
      <ShowableAtMount show={this.getModuleVisibility()}>
        { this.props.children }
      </ShowableAtMount>
    )
  }

}

export default ModuleComponent
