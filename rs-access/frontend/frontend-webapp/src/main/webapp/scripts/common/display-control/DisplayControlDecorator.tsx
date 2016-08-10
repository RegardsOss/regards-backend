/** @module Common.DisplayControl */
import * as React from "react"
import { connect } from "react-redux"
import ShowableAtRender from '../components/ShowableAtRender'
import { IDisplayController } from './types'
import HateoasDisplayController from './HateoasDisplayController'
import * as selectors from "../../reducer"

/**
 * Generic decorator for controlling display
 * Use this decorator on a React.Component class in order to control its display
 * with the passed controller logic.
 * You can also pass mapStateToProps/mapDispatchToProps methods in order to
 * connect the decorated component.
 *
 * @type {function}
 * @param {IAccessController} The controller in charge of supervising the display
 * @param {function} mapStateToProps method to connect the decorated component to the redux store
 * @param {function} mapDispatchToProps method to connect the decorated component to the dispatch
 * @return {React.Component<any, any>}
 */
export default function applyDisplayControl(controller: IDisplayController, mapStateToProps: any, mapDispatchToProps: any): any {

  return function (DecoratedComponent: React.ComponentClass<any>): any {

    class DisplayControlDecorator extends React.Component<any, any> {

      render(): JSX.Element {
        // Instanciate the component before in order to to pass it
        // to the controller
        const decoratedComponentElement = React.createElement(
          DecoratedComponent,
          this.props
        )

        return (
          <ShowableAtRender show={controller(decoratedComponentElement)} >
            {decoratedComponentElement}
          </ShowableAtRender>
        )
      }
    }

    return connect<any, any, any> (mapStateToProps, mapDispatchToProps) (DisplayControlDecorator)

  }

}

/**
 * Shortcut method to decorate a component with Hateoas display control logic
 *
 * @type {function}
 * @param {IAccessController} The controller in charge of supervising the display
 * @return {React.Component<any, any>}
 */
export function applyHateoasDisplayControl(DecoratedComponent: React.ComponentClass<any>): any {
  const mapStateToProps = (state: any) => ({
    endpoints: selectors.getEndpointsItems(state)
  })
  const mapDispatchToProps = (dispatch: any) => ({})
  return applyDisplayControl(HateoasDisplayController, mapStateToProps, mapDispatchToProps)(DecoratedComponent)
}
