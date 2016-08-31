import { HateoasLink } from "../hateoas/types"

export interface Project {
  name: string,
  links: Array<HateoasLink>
}
