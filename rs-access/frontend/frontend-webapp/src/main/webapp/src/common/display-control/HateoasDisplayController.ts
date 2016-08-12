import { IDisplayController } from './types'

/**
 * An Access Controller is a pure function which implements a logic for
 * displaying or not the passed React component.
 *
 * For HATEOAS, the component's "endpoints" prop must have an entry
 * of key "endpointKey" in order to be displayed
 *
 * For example:
 * endpointKey = "projects_url"
 * endpoints = {
 *  "projects_url": "http://myAwesomeUrl",
 *  "projects_users_url": "http://myOtherAwesomeUrl"
 * }
 *
 * @type {IAccessController}
 * @param {JSX.Element}
 * @return {boolean}
 */
const HateoasDisplayController: IDisplayController = (component: JSX.Element) => {
  return typeof component.props.endpoints[component.props.endpointKey] !== 'undefined'
}

export default HateoasDisplayController
