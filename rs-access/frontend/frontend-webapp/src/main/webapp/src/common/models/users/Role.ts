import { HateosLink } from "../hateos/types"
import { ResourceAccess } from "./ResourceAccess"


export interface Role {
  name: string,
  parentRole: Role,
  permissions: Array<ResourceAccess>
  links: Array<HateosLink>
}
