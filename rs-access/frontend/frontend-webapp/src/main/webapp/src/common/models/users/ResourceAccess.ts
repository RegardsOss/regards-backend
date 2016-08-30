import { HateosLink } from "../hateos/types"

export interface ResourceAccess {
  description: string,
  microservice: string,
  resource: string,
  verb: string
  links: Array<HateosLink>
}
