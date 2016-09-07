import { HateoasLink } from "../hateoas/types"

export interface ResourceAccess {
  description: string,
  microservice: string,
  resource: string,
  verb: string
  links: Array<HateoasLink>
}
