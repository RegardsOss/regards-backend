import * as React from 'react'
import ShowableAtMount from './ShowableAtMount'

interface ModuleComponentProps {
// TODO
}

interface ModuleComponentState {
// TODO
}

class ModuleComponent extends React.Component<ModuleComponentProps, ModuleComponentState> {
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
